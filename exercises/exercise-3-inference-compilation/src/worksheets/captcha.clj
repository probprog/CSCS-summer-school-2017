;; gorilla-repl.fileformat = 1

;; **
;;; # Exercise: Captcha
;;; 
;;; <p style="text-align:center">
;;; 	<img src="https://c1.staticflickr.com/8/7065/26946304530_cb30c23660_b.jpg" width=500/>
;;; </p>
;;; 
;;; You are the head of a [social hacking](https://en.wikipedia.org/wiki/Social_hacking) team that is trying to sway public's opinion on a controversial matter before an important election. You want to create as many Facebook accounts as possible and using those to post as many comments as possible supporting a particular position. You have managed to develop technology to identify which posts and comments to like and to generate your own comments.  However, creating Facebook accounts and posting many comments requires one thing that you currently can't do very well: solving Captchas. You don't have an army of labelers so supervised learning using deep neural networks is not going to cut it. You also want to capture any uncertainty we have in our predictions - if you are uncertain, you better not try to solve the Captcha unless you want to get blocked.
;;; 
;;; <p style="text-align:center">
;;; 	<img src="https://upload.wikimedia.org/wikipedia/commons/c/c4/2-Dice-Icon.svg" width=200/>
;;; </p>
;;; 
;;; Probabilistic programming and inference compilation to the rescue! By modelling (and inverting) the Captcha generative process you will be able to capture the uncertainty in your predictions and inference compilation is going to make this process much more efficient.
;; **

;; @@
(ns captcha
  (:require [anglican.runtime :refer :all]
            [anglican.emit :refer :all]
            [anglican.stat :as stat]
            [anglican.infcomp.zmq :as zmq]
            [anglican.inference :refer [infer]]
            [anglican.infcomp.prior :as prior]
            anglican.infcomp.core
            anglican.infcomp.csis
            anglican.rmh
            anglican.importance
            [gorilla-repl.image :as image]
            [clojure.core.matrix :as m]
            [gorilla-repl.table :as table])
  (:import [robots.OxCaptcha OxCaptcha]
           [javax.imageio ImageIO]
           [java.io File]))
;; @@

;; **
;;; In this worksheet, please complete everything that is marked with `...complete-this...`
;; **

;; @@
(def ...complete-this... nil)
;; @@

;; **
;;; ## Captcha stochastic renderer
;;; 
;;; We will try to break some captchas by doing inference over letter identities in a generative model containing both letter identities and the Captcha image (2D matrix of 0-255 numbers). The underlying renderer is written in Java. Since Clojure runs on the Java Virtual Machine, we can call Java functions directly.
;;; 
;;; Remember that the image is 50 pixels high and 200 pixels wide. x-axis goes from left to right and y-axis goes from top to bottom.
;;; 
;;; Play around with the parameters to get a feel for the renderer...
;; **

;; @@
;; Renderer setup
(def WIDTH 200) ; Width of CAPTCHA
(def HEIGHT 50) ; Height of CAPTCHA
(def oxCaptcha (OxCaptcha. WIDTH HEIGHT)) ; Renderer Java object
(def fonts ["Noto Sans" "Ubuntu"])
(defn stochastic-renderer [letters font-size kerning & {:keys [filename]}]
  (.background oxCaptcha)
  (.setFont oxCaptcha (rand-nth fonts) 0 font-size)
  (.text oxCaptcha (char-array letters) (+ 2 (rand-int 13)) (+ 29 (rand-int 4)) 1 (int kerning))
  (.distortionElastic oxCaptcha (+ 10 (rand-int 10)) 11 6)
  (.distortionShear2 oxCaptcha (rand-int WIDTH) (+ 10 (rand-int 30)) (+ -4 (rand-int 8)) (rand-int HEIGHT) (+ 5 (rand-int 15)) (+ -5 (rand-int 10)))
  (.noiseStrokes oxCaptcha (+ 6 (rand-int 5)) (+ 1.1 (rand 0.5)))
  (.noiseEllipses oxCaptcha (+ 4 (rand-int 3)) (+ 1.1 (rand 0.5)))
  (.noiseWhiteGaussian oxCaptcha 10)
  (when filename
    (.save oxCaptcha filename))
  (mapv #(into [] %) (seq (.getImageArray2D oxCaptcha))))

(let [filename "tmp/test.png"]
  (stochastic-renderer ...complete-this... :filename filename)
  (image/image-view (ImageIO/read (File. filename)) :type "png" :alt "captcha"))
;; @@

;; **
;;; You can also check the images in the folder `tmp/`. We can also get the actual matrix of values:
;; **

;; @@
(def test-captcha (stochastic-renderer ...complete-this...))
(m/shape test-captcha)
;; @@

;; **
;;; ## Approximate Bayesian computation in probabilistic programming
;;; 
;;; Our generative model will have the form of
;;; 
;;; \begin{align}
;;; 	\theta := (\text{num-letters}, \text{letters}, \text{fontsize}, \text{kerning}) &\sim p(\theta) \\\\
;;;     y := \text{captcha-image} &\sim p(y | \theta).
;;; \end{align}
;;; 
;;; Using the backend inference algorithms, we would like to find the posterior @@p(\theta | y)@@.
;;; 
;;; In order to do this, we need to specify the prior @@p(\theta)@@ and the likelihood @@p(y | \theta)@@ in our probabilistic program.
;;; Since the Captcha image is generated using a stochastic renderer, its likelihood is intractable and we resort to approximate Bayesian computation (ABC).
;;; In a probabilistic programming setting, we can incorporate the ABC distance into our inference scheme by designing a custom, improper, distribution object that takes as parameters
;;; 
;;; - rendered Captcha render(@@\theta@@) and
;;; - the ABC closeness tolerance.
;;; 
;;; To define a custom distribution object, we need to implement its sampling mechanism and the calculation of its log density.
;;; The sampling mechanism of this distribution just returns render(@@\theta@@) and log density just returns the ABC distance which should be high when the rendered Captcha render (@@\theta@@) is similar to the observed one and low otherwise.
;;; 
;;; ### One Possible ABC distance
;;; Recalling that render(@@\theta@@) and @@y@@ are both @@50 \times 200@@ integer matrices, we can for example design an ABC distance @@\Delta@@ as follows:
;;; \begin{align}
;;; 	\Delta(y, \text{render}(\theta)) = \text{Normal}(\text{flatten}(y); \text{flatten}(\text{render}(\theta)), \sigma^2 I)
;;; \end{align}
;;; where `flatten` flattens matrices into a vector and @@\sigma@@ is a parameter chosen by us.
;;; 
;;; In this example, we will use a very similar ABC distance:
;;; \begin{align}
;;; 	\Delta(y, \text{render}(\theta)) = \text{Normal}(R \cdot \text{flatten}(y); R \cdot \text{flatten}(\text{render}(\theta)), \sigma^2 I).
;;; \end{align}
;;; The difference is that we map from the flattened Captcha image to a lower dimensional feature vector by multiplying the flattened, @@10000@@-dimensional vector by a random projection matrix @@R \in \mathbb R^{500 \times 10000}@@ (see section 2.1 in [here](http://www.ime.unicamp.br/~wanderson/Artigos/randon_projection_kdd.pdf)).
;;; The feature vector approximates the sufficient statistics of the Captcha image.
;;; The reason for this is to make the posterior space "smoother".
;;; This is not the most ideal sufficient statistic extractor but it is sufficient for demonstration purposes.
;;; A better but harder-to-implement alternative would be to use, for example, the [VGG feature extractor](https://github.com/coreylynch/vgg-19-feature-extractor) instead.
;;; 
;;; The following script has code for generating the random projection matrix but for reproducibility, we load a pre-generated matrix from disk. The following script can take some time.
;; **

;; @@
;; Dimensionality reduction for abc-dist
(defdist random-projection-matrix
  "Returns the [k d] matrix R described in http://www.ime.unicamp.br/~wanderson/Artigos/randon_projection_kdd.pdf"
  [k d]
  [dist (categorical [[(sqrt (/ 3. k)) (/ 1 6)] [0. (/ 2. 3)] [(- (sqrt (/ 3. k))) (/ 1 6)]])]
  (sample* [this] (repeatedly k (fn [] (repeatedly d #(sample* dist)))))
  (observe* [this value] nil))

(defn gen-random-projection-matrix [source-dim target-dim]
  (sample* (random-projection-matrix target-dim source-dim)))

(def FEATURE-DIM 500)
(def CAPTCHA-DIM 10000)


;(let [random-projection-matrix (gen-random-projection-matrix CAPTCHA-DIM FEATURE-DIM)]
;  (spit "resources/random-projection-matrix.csv" (clojure.string/join "\n" (mapv #(clojure.string/join "," %) random-projection-matrix))))

; The commented out block above is the code to generate a random projection matrix. For reproducibility, we will just load one from disk:
(def random-projection-matrix (m/matrix (mapv (fn [line]
                                                (mapv read-string (clojure.string/split line #",")))
                                              (clojure.string/split-lines (slurp "resources/random-projection-matrix.csv")))))

(defn reduce-dim [captcha]
  (m/mmul random-projection-matrix (m/to-vector (flatten captcha))))

(println "Shape of the feature vector: " (m/shape (reduce-dim test-captcha)))
;; @@

;; **
;;; ### Custom Anglican distribution object for ABC distance
;;; The following script uses `defdist` to define a custom Anglican distribution object `abc-dist` which acts as the ABC distance. It takes `rendered-image` and `abc-sigma` as arguments. `abc-sigma` corresponds to @@\sigma@@ above.
;;; 
;;; When we **sample** from `abc-dist`, we obtain `rendered-image`.
;;; 
;;; When we **observe** an `observed-captcha-image` under this distribution, the log of the ABC distance described above will be calculated and factored into the trace.
;; **

;; @@
(defdist abc-dist
  "Approximate Bayesian Computation distribution constructor. Reduces dimensions using Random projections and then calculates likelihood under diagonal multivariate Gaussian under the rendered image."
  [rendered-image abc-sigma]
  [dist (normal 0 abc-sigma)]
  (sample* [this] rendered-image)
  (observe* [this observed-image]
            (reduce + (map #(observe* dist (- %1 %2))
                           (reduce-dim observed-image)
                           (reduce-dim rendered-image)))))
;; @@

;; **
;;; ## The Captcha probabilistic program
;;; 
;;; Fill in the necessary blanks in the program in order to form a generative model. A suggested format of the program is provided:
;; **

;; @@
(def abc-sigma 1)
(def letter-dict "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789") ; Dictionary mapping from letter-id to letter
(anglican.infcomp.core/reset-infcomp-addressing-scheme!)
(with-primitive-procedures [stochastic-renderer abc-dist]
  (defquery captcha [baseline-image]
    (let [num-letters ...complete-this...
          font-size ...complete-this...
          kerning ...complete-this...
          letters ...complete-this...

          ;; Render image using renderer from ...
          rendered-image ...complete-this...]

      ;; ABC-style observe
      (observe (abc-dist rendered-image abc-sigma) baseline-image)

      ;; Returns
      {:letters letters
       :font-size font-size
       :kerning kerning})))
;; @@

;; **
;;; Here are some samples of the captcha from the prior:
;; **

;; @@
(def num-captchas-from-prior 5)
(def samples-from-prior (take num-captchas-from-prior (prior/sample-from-prior captcha nil)))
(def captchas-from-prior (map #(:value (first (:observes %))) samples-from-prior))

(map (fn [captcha-from-prior]
       (let [filename "tmp/captcha-from-prior.png"]
         (.save oxCaptcha (int-array (flatten captcha-from-prior)) WIDTH HEIGHT filename)
         (image/image-view (ImageIO/read (File. filename)) :type "png")))
     captchas-from-prior)
;; @@

;; **
;;; ## Inference Compilation
;;; 
;;; We will now compile away the cost of inference in this model by training a neural network that maps from the observed Captcha to efficient importance sampling proposals for @@\theta@@.
;;; 
;;; First, we need to provide a function that combines observations generated from the prior and returns an ND-array. The observations generated from prior look like this in this model (in other models, they would look differently). Note that the `[nil]` corresponds to a dummy, unused, observed Captcha because the `observe` statement in the query will be used to sample a Captcha instead of factoring its ABC likelihood:
;; **

;; @@
(prior/sample-observes-from-prior captcha [nil])
;; @@

;; **
;;; Write a function that takes the above observations and returns an ND array (in this case 2D matrix):
;; **

;; @@
(defn combine-observes-fn [observes]
  ...complete-me...)
;; @@

;; **
;;; We can test this function on one sample; the following script should return `[50 200]`:
;; **

;; @@
(m/shape (combine-observes-fn (prior/sample-observes-from-prior captcha [nil])))
;; @@

;; **
;;; Now, start the server that will produce samples from the joint distribution:
;; **

;; @@
(def replier (zmq/start-replier captcha [nil] combine-observes-fn))
;; @@

;; **
;;; and start the neural network which will train using these samples by running the following script in a location of your choosing:
;;; 
;;; `python -m pyprob.compile --obsEmb cnn2d6c --obsEmbDim 1024 --validSize 1 --batchSize 1 --standardize`
;;; 
;;; This will start the PyTorch-based neural network training using training data sampled from the prior of the `captcha` probabilistic program written in this worksheet. `--obsEmb cnn2d6c` specifies the observation embedding used to compile this model. `--obsEmbDim 1024` sets the dimensionality of the observation embedding to 1024. `--standardize` standardizes the observation to have mean 0 and standard deviation 1 which is a standard trick in vision-based neural network training tasks. `--validSize 1 --batchSize 1` sets the validation set size and batch size to a conservative size of 1 so that we don't run out of memory.
;;; 
;;; The training (compilation) time for this model is usually a few days and we won't have time to fully train our compilation artifact. Nevertheless, let the training run for a while and then stop the Python script using Ctrl+C and stop the ZMQ connection by running the following cell:
;; **

;; @@
(zmq/stop-replier replier)
;; @@

;; **
;;; ## Generate some Captchas on which we want to do inference
;; **

;; @@
(def ground-truth-letters ["fhywFm" "Tw9C4Sk" "xCP9RQ"])
(def test-captchas (map #(stochastic-renderer % (+ 38 (rand-int 6)) (- (rand-int 4) 2)) ground-truth-letters))

(def test-captcha-images
  (map (fn [test-captcha]
       (let [filename "tmp/test-captcha.png"]
         (.save oxCaptcha (int-array (flatten test-captcha)) WIDTH HEIGHT filename)
         (image/image-view (ImageIO/read (File. filename)):type "png")))
     test-captchas))

test-captcha-images
;; @@

;; **
;;; ## Inference
;; **

;; **
;;; Perform importance sampling inference and take the maximum a-posteriori (MAP) estimate. Given a set of weighted particles @@(w\_k, x\_k)\_{k = 1}^K@@, the MAP estimate is just @@x\_{\mathrm{argmax}_k w\_k}@@.
;; **

;; @@
(defn empirical-MAP
  "calculates a maximum a-posteriori value from weighted samples;
  - accepts a map or sequence of log weighted
  values [x log-w].
  - returns the x with largest log-w"
  [weighted]
  (first (apply max-key second weighted)))

(def num-particles 10)
(def is-states-list (map (fn [observe]
                           (take num-particles (infer :importance captcha [observe])))
                         test-captchas))
(def is-MAP-list (map (comp empirical-MAP stat/collect-results) is-states-list))
(def is-captcha-images (map (fn [is-MAP filename]
                              (stochastic-renderer (:letters is-MAP)
                                                   (:font-size is-MAP)
                                                   (:kerning is-MAP)
                                                   :filename filename)
                              (image/image-view (ImageIO/read (File. filename)) :type "png"))
                            is-MAP-list
                            (map #(str "tmp/" % "-is.png") (range 1 (inc (count test-captchas))))))

(table/table-view
  (map (fn [is-captcha-image test-captcha-image]
         [is-captcha-image test-captcha-image])
       is-captcha-images
       test-captcha-images)
  :columns ['Importance 'Ground])
;; @@

;; **
;;; Perform inference using the random-walk Metropolis Hastings (`rmh`) algorithm which is a variant of the Markov Chain Monte Carlo and use the sample from the last iteration as an estimate for the latent variables in the Captcha model:
;; **

;; @@
(def num-iters 10)
(def rmh-states-list (map (fn [observe]
                            (take num-iters (infer :rmh captcha [observe])))
                          test-captchas))
(def rmh-posterior-list (map (comp first last stat/collect-results) rmh-states-list))

(def rmh-captcha-images (map (fn [rmh-posterior filename]
                               (stochastic-renderer (:letters rmh-posterior)
                                                    (:font-size rmh-posterior)
                                                    (:kerning rmh-posterior)
                                                    :filename filename)
                               (image/image-view (ImageIO/read (File. filename)) :type "png"))
                             rmh-posterior-list
                             (map #(str "tmp/" % "-rmh.png") (range 1 (inc (count test-captchas))))))

(table/table-view
  (map (fn [rmh-captcha-image test-captcha-image]
         [rmh-captcha-image test-captcha-image])
       rmh-captcha-images
       test-captcha-images)
  :columns ['RMH 'Ground])
;; @@

;; **
;;; Run importance sampling using the proposals from the neural network.
;;; 
;;; Before running the following block, run the neural network proposal server from `resources/compiled-artifacts/`
;;; 
;;; ```
;;; python -m pyprob.infer
;;; ```
;;; 
;;; which will start the PyTorch-based inference server for running the neural network during inference.
;; **

;; @@
(def num-particles 10)
(def csis-states-list (map (fn [observe]
                             (take num-particles (infer :csis captcha [observe])))
                           test-captchas))
(def csis-MAP-list (map (comp empirical-MAP stat/collect-results) csis-states-list))
(def csis-captcha-images (map (fn [csis-MAP filename]
                                (stochastic-renderer (:letters csis-MAP)
                                                     (:font-size csis-MAP)
                                                     (:kerning csis-MAP)
                                                     :filename filename)
                                (image/image-view (ImageIO/read (File. filename)) :type "png"))
                              csis-MAP-list
                              (map #(str "tmp/" % "-csis.png") (range 1 (inc (count test-captchas))))))

(table/table-view
  (map (fn [csis-captcha-image test-captcha-image]
         [csis-captcha-image test-captcha-image])
       csis-captcha-images
       test-captcha-images)
  :columns ['CSIS 'Ground])
;; @@

;; **
;;; View recognition rates and [Levenshtein distances](https://en.wikipedia.org/wiki/Levenshtein_distance) from the ground truth:
;; **

;; @@
(def levenshtein
  (memoize (fn [str1 str2]
             (let [len1 (count str1)
                   len2 (count str2)]
               (cond (zero? len1) len2
                     (zero? len2) len1
                     :else
                     (let [cost (if (= (first str1) (first str2)) 0 1)]
                       (min (inc (levenshtein (rest str1) str2))
                            (inc (levenshtein str1 (rest str2)))
                            (+ cost
                               (levenshtein (rest str1) (rest str2))))))))))
(defn levenshtein-normalized [str1 str2]
  (let [len1 (count str1)
        len2 (count str2)]
    (float (/ (levenshtein str1 str2) (max len1 len2)))))

(def is-letters (map :letters is-MAP-list))
(def rmh-letters (map :letters rmh-posterior-list))
(def csis-letters (map :letters csis-MAP-list))
(table/table-view (m/transpose [ground-truth-letters is-letters rmh-letters csis-letters]) :columns ['Ground 'Importance 'RMH 'CSIS])


; Recognition rates
(def is-rate (* 100 (/ (count (filter identity (map = ground-truth-letters is-letters))) (count ground-truth-letters))))
(def rmh-rate (* 100 (/ (count (filter identity (map = ground-truth-letters rmh-letters))) (count ground-truth-letters))))
(def csis-rate (* 100 (/ (count (filter identity (map = ground-truth-letters csis-letters))) (count ground-truth-letters))))

; Levenshtein distances
(def is-levenshtein (mean (map levenshtein-normalized ground-truth-letters is-letters)))
(def rmh-levenshtein (mean (map levenshtein-normalized ground-truth-letters rmh-letters)))
(def csis-levenshtein (mean (map levenshtein-normalized ground-truth-letters csis-letters)))

(table/table-view [[(symbol "Recognition rate") is-rate rmh-rate csis-rate]
                   [(symbol "Mean Levenshtein distance (from 0 to 1; the lower the better)") is-levenshtein rmh-levenshtein csis-levenshtein]]
                  :columns [(symbol "") 'Importance 'RMH 'CSIS])
;; @@
