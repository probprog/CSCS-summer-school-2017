(ns exercises.captcha
  (:refer-clojure :exclude [rand rand-nth rand-int name read])
  (:require [clojure.string :as str]
            [clojure.core.matrix :as m]
            [clojure.java.io :as io])
  (:use clj-hdf5.core
        [anglican runtime emit core inference])
  (:import (ch.systemsx.cisd.hdf5 HDF5Factory IHDF5SimpleReader
                                  IHDF5SimpleWriter HDF5FactoryProvider
                                  HDF5DataClass HDF5StorageLayout)
           [robots.OxCaptcha OxCaptcha]))

;; H5
(defn- array-to-mat-vec [array]
  "Takes in either 1D or 2D Java array and returns either Clojure vector or matrix."
  (let [temp (seq array)]
    (if (number? (first temp))
      (m/to-vector temp)
      (m/matrix (map seq temp)))))

(defn- parse-datasets-hdf5 [filename]
  "Takes filename of HDF5 file containing only 1D and 2D datasets,

  Returns hashmap of the same structure as the group structure with
  hashmap keys being group names."
  (let [h5root (open (clojure.java.io/file filename))
        datasets-and-paths (remove nil? (doall (walk h5root #(if (dataset? %)
                                                               {:dataset (read %) :path (:path %)}
                                                               nil))))
        result (reduce (fn [nn-params dataset-and-path]
                         (let [dataset (array-to-mat-vec (:dataset dataset-and-path))
                               path (map read-string (remove #(= % "")
                                                             (str/split (:path dataset-and-path) #"/")))]
                           (assoc-in nn-params path dataset)))
                       {}
                       datasets-and-paths)]
    (close h5root)
    result))

;; Reading random projection matrices
(def random-projection-matrices (parse-datasets-hdf5 "resources/random-projection-matrices.h5"))
(def dataset-name 'R7500-500) ; Possible datasets: 'R100, 'R200, 'R500, 'R1000, 'R2000. The number is the target dimension number.
(def R (m/matrix (get random-projection-matrices dataset-name))) ; [original-dim target-dim] matrix
(defn reduce-dim [captcha]
  (m/mmul R (m/to-vector (apply concat captcha))))

;; Renderer setup
(def WIDTH 150) ; Width of CAPTCHA
(def HEIGHT 50) ; Height of CAPTCHA
(def avg-width 20) ; Average width of CAPTCHA letter
(def avg-height 25) ; Average height of CAPTCHA letter

(def oxCaptcha (OxCaptcha. WIDTH HEIGHT)) ; Renderer object
(defn render [xs ys letters salt-and-pepper & {:keys [mode] :or {mode OxCaptcha/RELATIVE}}]
  (.background oxCaptcha)
  (.text oxCaptcha (char-array letters) (int-array xs) (int-array ys) mode)
  (.blurGaussian oxCaptcha 2 2.0)
  (if salt-and-pepper (.noiseSaltPepper oxCaptcha))
  (mapv #(into [] %) (seq (.getImageArray2D oxCaptcha))))
(defn render-to-file [xs ys letters salt-and-pepper filename & {:keys [mode] :or {mode OxCaptcha/RELATIVE}}]
  (.background oxCaptcha)
  (.text oxCaptcha (char-array letters) (int-array xs) (int-array ys) mode)
  (.blurGaussian oxCaptcha 2 2.0)
  (if salt-and-pepper (.noiseSaltPepper oxCaptcha))
  (.save oxCaptcha filename))
(defn index-of-sorted [v]
  (mapv first (sort-by second (map-indexed vector v))))

(defdist abc-dist
  "Approximate Bayesian Computation distribution constructor. Reduces dimensions using Random projections and then calculates likelihood under diagonal multivariate Gaussian under the rendered image."
  [rendered-image abc-sigma]
  [dist (normal 0 abc-sigma)]
  (sample* [this] rendered-image)
  (observe* [this baseline-image]
           (reduce + (map #(observe* dist (- %1 %2))
                          (reduce-dim baseline-image)
                          (reduce-dim rendered-image)))))

(defdist overlap-abc-dist
  [avg-width penalty] []
  (sample* [this] nil)
  (observe* [this ordered-xs]
           (reduce + (map #(let [diff (- (second %) (first %))]
                             (if (< diff avg-width)
                               (* penalty (log (/ diff avg-width)))
                               0))
                          (partition 2 1 ordered-xs)))))

(defn retain-visible [indices visible]
  (remove nil? (map #(if (nth visible %) %) indices)))

(with-primitive-procedures [render abc-dist]
  (defquery captcha-with-offsets [letter-dict]
    (let [num-letters (sample "numletters" (uniform-discrete 3 6))
          [x-offsets y-offsets letter-ids] (loop [x-offsets []
                                                  y-offsets []
                                                  letter-ids []]
                                   (if (= (count x-offsets) num-letters)
                                     [x-offsets y-offsets letter-ids]
                                     (let [num-sofar (count x-offsets)
                                           last-x (+ (apply + x-offsets) (* num-sofar avg-width))
                                           num-left (- num-letters num-sofar)
                                           max-length (- WIDTH (* num-left avg-width) last-x)
                                           x-offset (round (* max-length (sample "xoffset" (beta 1 3))))
                                           y-offset (round
                                                     (+ (if (empty? y-offsets)
                                                          (/ (+ HEIGHT avg-height) 2)
                                                          (- (reduce + y-offsets)
                                                             (first y-offsets)))
                                                        (sample "yoffset" (normal 0 (/ avg-height 30)))))
                                           letter-id (sample "letterid" (uniform-discrete 0 (count letter-dict)))]
                                       (recur (conj x-offsets x-offset)
                                              (conj y-offsets y-offset)
                                              (conj letter-ids letter-id)))))

          letters (apply str (map (partial nth letter-dict) letter-ids))

          ;; Render image using renderer from ...
          rendered-image (render x-offsets y-offsets letters false :mode OxCaptcha/RELATIVE)]

      ;; ABC-style observe
      {:letters letters
       :rendered-image rendered-image})))

(defn generate-test-samples [letter-dict num-observes folder]
  (let [prior-dist (conditional captcha-with-offsets :lmh)
        test-samples (repeatedly num-observes  #(sample* (prior-dist letter-dict)))
        letters (map :letters test-samples)
        observes (map :rendered-image test-samples)]
    (doall (map (fn [i]
              (let [obs (nth observes i)]
                (.save oxCaptcha
                       (int-array (reduce concat obs))
                       WIDTH
                       HEIGHT
                       (str folder "captcha-" (inc i) "-ground.png"))))
            (range (count observes))))
    {:letters letters :observes observes}))

;; Inference helpers
;; Helpers to obtain Maximum a Posteriori (MAP)
(defn min-index [v]
  (let [length (count v)]
    (loop [minimum (first v)
           min-index 0
           i 1]
      (if (< i length)
        (let [value (nth v i)]
          (if (< value minimum)
            (recur value i (inc i))
            (recur minimum min-index (inc i))))
        min-index))))

(defn max-index [v]
  (min-index (mapv - v)))

;; Helpers
(defn- smc-captcha-posterior-states [query num-particles value]
  (take num-particles (infer :smc
                             query
                             value)))

(defn smc-captcha-MAP-state [query num-particles value]
  (let [states (smc-captcha-posterior-states query num-particles value)
        log-weights (map :log-weight states)]
    (nth states (max-index log-weights))))

(defn- smc-captcha-posterior-state [query num-particles value]
  (last (smc-captcha-posterior-states query num-particles value)))

(defn rmh-captcha-posterior-state [query num-iters value]
  (nth (infer :rmh query value) (dec num-iters)))

(defn extract-from-state [state filename]
  (let [result (:result state)
        log-weight (anglican.state/get-log-weight state)]
    (.save oxCaptcha
           (int-array (reduce concat (:rendered-image result)))
           WIDTH
           HEIGHT
           filename)
    (assoc result :log-weight log-weight)))

(defn smc-captcha-MAP-state [query num-particles value]
  (let [states (smc-captcha-posterior-states query num-particles value)
        log-weights (map :log-weight states)]
    (nth states (max-index log-weights))))
