;; gorilla-repl.fileformat = 1

;; **
;;; # Exercise 4: Program Traces And Inference
;;; 
;;; Authors:
;;; 
;;;  - Brooks Paige [brooks@robots.ox.ac.uk](mailto:brooks@robots.ox.ac.uk)
;;;  - Frank Wood [fwood@robots.ox.ac.uk](mailto:fwood@robots.ox.ac.uk)
;; **

;; @@
(ns poisson-trace
  (:require [gorilla-plot.core :as plot]
            [gorilla-repl.image :as image]
            [clojure.java.io :as io])
  (:import [javax.imageio ImageIO])
  (:use [anglican core emit runtime]))
;; @@

;; **
;;; In this exercise we will write a rejection sampler which draws from a Poisson distribution.
;;; A Poisson distribution has a probability mass function which looks like this:
;;; 
;;; <img src="https://upload.wikimedia.org/wikipedia/commons/1/16/Poisson_pmf.svg">
;;; 
;;; If you aren't familiar with the Poisson distribution, you can 
;;; [read about it here](https://en.wikipedia.org/wiki/Poisson_distribution).
;;; 
;;; An algorithm to sample a Poisson random variate @@k@@ with rate @@\lambda@@ is given by [Knuth, 1969].
;;; Initialize @@L \leftarrow e^{-\lambda}@@, @@k \leftarrow 0@@, and @@p \leftarrow 1@@, and then loop:
;;; 
;;; 1. Update @@k \leftarrow k+1@@
;;; 2. Sample @@u \sim \mathrm{Uniform}(0, 1)@@
;;; 3. Update @@p \leftarrow p \times u@@
;;; 4. If @@p \le L@@ return @@k -1@@; otherwise repeat.
;;; 
;;; This algorithm increments @@k@@ until eventually returning.
;;; 
;;; To start, implement this algorithm for sampling from a Poisson distribution as an Anglican program,
;;; and predict:
;;; 
;;; * `:k`, the poisson variate,
;;; * `:trace`, a vector of length `:k` containing the sampled uniform random variates,
;;; * `:large?`, a boolean value which is true when @@k > 3@@.
;;; 
;; **

;; @@
(defquery poisson-trace [lambda]
  
  ;;; *** YOUR CODE HERE *** ;;;

  {:large? false
   :k 0
   :trace []})
;; @@

;; **
;;; The following code will run a Metropolis-Hastings algorithm to sample from the `poisson-trace` probabilistic program.
;;; 
;;; We can see from the histogram that we are, indeed, sampling from @@\mathrm{Poisson}(4)@@:
;; **

;; @@
(def poisson-lmh 
  (map :result
       (doquery :lmh poisson-trace [4])))

(plot/compose 
  (plot/histogram (repeatedly 10000 #(sample* (poisson 4))) 
                  :bins 100 :normalize :probability :colour "red")
  (plot/histogram (map :k (take 10000 poisson-lmh))
                  :bins 100 :normalize :probability))
;; @@

;; **
;;; We can use samples from this Anglican program to predict @@p(k > 3)@@:
;; **

;; @@
(def large-ratio 
  (mean (map #(if (:large? %) 1 0) (take 10000 poisson-lmh))))

(println "[Q] What is p(k > 3)?\n[A] Exact:  0.5665 \n[A] Estim: " large-ratio)
;; @@

;; **
;;; ## Program execution traces
;;; 
;;; A program execution trace includes all random choices made in the execution of the program.  The _prior_ probability of an execution trace is defined as the product of the probabilities of those choices, in this program
;;; 
;;; $$\begin{align}
;;; p(\mathrm{trace}) &= \prod\_{i=1}^{k+1} p(u\_i)
;;; \end{align}$$
;;; 
;;; where @@u\_1, \dots, u\_{k+1}@@ is the sequence of uniform random draws made during a single execution of the sampling procedure.
;;; 
;;; Note that each run of the program generates an execution trace with a possibly different amount of randomness;
;;; that is, sampling a value of @@k=10@@ requires more random choices than a value of @@k = 5@@.
;;; If @@\lambda = 4@@, then under a Poisson distribution the probability masses @@p(k=3) = p(k=4)@@, but in this algorithm generating a @@k = 4@@ variate requires more random choices.
;;; 
;;; Particularly vexing, each random choice is a draw from Uniform@@(0, 1)@@ then for each @@u\_i@@ we have @@p(u\_i) = 1@@.
;;; 
;;; This is illustrated in the following diagram, showing sequential steps during the execution of the sampler program.
;;; Execution flows to the right; each column shows a random draw @@u\_i \sim \mathrm{Uniform}(0, 1)@@.
;;; Green arrows represent the program branching that occurs at the end of each algorithm iteration.
;;; Red arrows denote program termination, with a given return value of @@k@@.
;; **

;; @@
(image/image-view (ImageIO/read (io/resource "execution-diagram.png"))
                  :type "png" :alt "Execution trace")
;; @@

;; **
;;; Since the probability of the program execution trace is defined as the product of the probabilities of all random choices made in the execution of the program,
;;; then each execution of the program has the same probability, regardless of @@k@@, with
;;; 
;;; $$\begin{align}
;;; p(\mathrm{trace}) &= \prod\_{i=1}^{k+1} p(u\_i) = 1
;;; \end{align}$$
;;; 
;;; for all @@k = 0, 1, 2, \dots@@.
;;; The probability of sampling a particular value @@k@@ is directly related to the length of the program execution trace --
;;; the algorithm is defined in such a way that, although each individual execution trace has equal probability, the number of execution traces which terminate after @@k@@ total uniform draws is proportional to a Poisson distribution with rate @@\lambda@@.
;;; 
;;; What do these traces actually look like? The following function prints the predicted `:trace`.
;; **

;; @@
(defn format-trace [smp]
  (str "k = " (:k smp) ", trace: " (mapv #(format "%.2f" %) (:trace smp)) "\n"))

(println (map format-trace (take 20 poisson-lmh)))
;; @@

;; **
;;; Recall how the lightweight Metropolis-Hastings algorithm for probabilistic programs [Wingate et al, 2011] operates. 
;;; 
;;; * Can you explain this output? 
;;; * Why do subsequent samples share parts of the same trace?
;;; 
;;; Compare this with the traces sampled from PIMH-based algorithms. In a PIMH we perform successive SMC sweeps, which are accepted or rejected according to their average importance weight (i.e. the estimate of the marginal likelihood). In a program without observes PIMH generates a sequence of independent samples (we will in a moment consider a program with observes)
;; **

;; @@
(def poisson-pimh 
  (map :result
       (doquery :pimh poisson-trace [4] 
                :number-of-particles 10)))

(println (map format-trace (take 20 poisson-pimh)))
;; @@

;; **
;;; Convince yourself that the PIMH-based algorithm here is also sampling from the appropriate distribution.
;; **

;; @@
(plot/histogram (map :k (take 10000 poisson-pimh)) 
                :bins 100 :normalize :probability)
;; @@

;; **
;;; Instead of plotting the distribution over @@k@@, we can also plot the distribution over the trace itself! 
;;; 
;;; The following function plots histograms of the marginal distributions over @@u\_i@@, for the first 6 uniform draws, conditioned on their existence within the trace. That is, each histogram element shows @@p(u\_i|k \ge i)@@.
;; **

;; @@
(defn plot-trace-histogram [num-samples sampler]
  (let [traces (map :trace (take num-samples sampler))]
    (mapv (fn [entry] 
            (plot/histogram (filter identity (map #(get % entry) traces))
                            :plot-size 200
                            :normalize :probability-density
                            :plot-range [[0 1] :all]))
          (range 6))))
;; @@

;; **
;;; Before looking at the following histograms, consider:
;;; 
;;; * What marginal distributions would you expect to see for each @@u\_i@@?
;;; * How would you expect the marginal distribution of @@u\_i@@ to change across @@i@@?
;; **

;; @@
(plot-trace-histogram 10000 poisson-pimh)
;; @@

;; **
;;; # Tilted Poisson sampler: adding conditioning
;;; 
;;; Now, we alter the model by adding an observe statement. Suppose we require that @@k > 3@@ with probability @@0.9@@.
;;; 
;;; We write our new model `tilted-poisson` with Anglican code identical to our Poisson sampler, but adding in an `observe` which enforces @@p(k > 3) = 0.9@@:
;; **

;; @@
(defquery tilted-poisson [lambda]
  
  ;;; *** YOUR CODE HERE *** ;;;

  ;;; HINT: this should only be a single line change from your previous `poisson-trace` query!

  {:large? false
   :k 0
   :trace []})
;; @@

;; **
;;; * What does the posterior distribution over @@k@@ look like in this model?
;; **

;; @@
(def tilted-pimh 
  (map :result
       (doquery :pimh tilted-poisson [4] 
                :number-of-particles 10)))

(plot/histogram (map :k (take 10000 tilted-pimh))
                :bins 100 :normalize :probability)
;; @@

;; **
;;; In addition to looking at the posterior on @@k@@, we can look at the posterior on the trace itself.
;;; 
;;; Before looking at the following histograms,
;;; 
;;; * Do you expect the posterior distribution over each @@u\_i@@ to be the same as in the unconditioned Poisson model?
;;; * Do you expect the posterior over each @@u\_i@@ will vary across @@i@@?
;; **

;; @@
(plot-trace-histogram 10000 tilted-pimh)
;; @@
