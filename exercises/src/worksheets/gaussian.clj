;; gorilla-repl.fileformat = 1

;; **
;;; # Gaussian
;; **

;; **
;;; Import the minimum required libraries to run define queries, compile them, and run the inference using compiled artifacts.
;; **

;; @@
(ns worksheets.gaussian
  (:require [anglican.runtime :refer :all]
            [anglican.emit :refer [defquery]]
            [anglican.stat :as stat]
            [anglican.infcomp.zmq :as zmq]
            [anglican.inference :refer [infer]]
            [anglican.infcomp.prior :as prior]
            [gorilla-plot.core :as plt]
            anglican.infcomp.csis
            anglican.importance
            anglican.infcomp.core))

(anglican.infcomp.core/reset-infcomp-addressing-scheme!)
;; @@

;; **
;;; Define a Gaussian unknown mean model
;; **

;; @@
(def prior-mean 0)
(def prior-std 1)
(def observation-std 0.1)

(defquery gaussian [obs]
  (let [mean (sample (normal prior-mean prior-std))
        std observation-std]
    (observe (normal mean std) obs)
    mean))
;; @@

;; **
;;; ## Inference
;;;
;;; Analytic posterior
;; **

;; **
;;; Specify a function `combine-observes-fn` that combines observes from a sample in a form suitable for Torch:
;; **

;; @@
(defn combine-observes-fn [observes]
  (:value (first observes)))
;; @@

;; **
;;; In order to write this function, you'll need to look at how a typical `observes` object from your query looks like:
;; **

;; @@
(prior/sample-observes-from-prior gaussian [nil])
;; @@

;; **
;;; Compile our query
;; **

;; @@
(def replier (zmq/start-replier gaussian [nil] combine-observes-fn))
;; @@

;; **
;;; Then run the following to train the neural network:
;;;
;;; `python -m infcomp.compile`
;; **

;; @@
(zmq/stop-replier replier)
;; @@

;; **
;;; ## Inference
;; **

;; @@
(defn posterior-mean-std [prior-mean prior-std observation-std test-observations]
  (let [prior-variance (* prior-std prior-std)
        observation-variance (* observation-std observation-std)
        num-observations (count test-observations)
        posterior-mean (+ (/ (* observation-variance prior-mean)
                              (+ (* num-observations prior-variance) observation-variance))
                           (/ (* num-observations prior-variance (/ (reduce + test-observations) num-observations))
                              (+ (* num-observations prior-variance) observation-variance)))
        posterior-variance (/ 1 (+ (/ 1 prior-variance) (/ num-observations observation-variance)))]
    [posterior-mean (Math/sqrt posterior-variance)]))

(def test-observation 2.3)
(def posterior-dist (apply normal (posterior-mean-std prior-mean prior-std observation-std [test-observation])))
(def posterior-plot (plt/plot #(Math/exp (observe* posterior-dist %)) [-3 3]))
;; @@

;; **
;;; First, run the inference server from torch-infcomp:
;;;
;;; `python -m infcomp.infer`
;;;
;;; Then run the query, specifying number of particles:
;; **

;; @@
(def num-particles 100)
(def csis-states (take num-particles (infer :csis gaussian [test-observation])))
(plt/compose
  posterior-plot
  (plt/histogram (repeatedly 10000 #(sample* (categorical (vec (stat/empirical-distribution (stat/collect-results csis-states)))))) :normalize :probability-density))
;; @@

;; **
;;; Using the same number of particles, we don't have such good results with importance sampling:
;; **

;; @@
(def num-particles 100)
(def is-states (take num-particles (infer :importance gaussian [test-observation])))
(plt/compose
  posterior-plot
  (plt/histogram (repeatedly 10000 #(sample* (categorical (vec (stat/empirical-distribution (stat/collect-results is-states)))))) :normalize :probability-density))
;; @@
