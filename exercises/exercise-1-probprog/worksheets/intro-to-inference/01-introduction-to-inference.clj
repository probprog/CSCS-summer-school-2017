;; gorilla-repl.fileformat = 1

;; **
;;; # Exercises: Bayesian Posterior Inference
;; **

;; @@
(ns posterior-inference-exercises
  (:require [anglican.runtime :refer [mean]]
            [gorilla-plot.core :as plot]))

(defn normal-logpdf [x mu sigma]
  (if (> sigma 0)
    (anglican.runtime/observe* (anglican.runtime/normal mu sigma) x)
    (Math/log 0.0)))

(defn randn [mu sigma]
  (anglican.runtime/sample* (anglican.runtime/normal mu sigma)))

(defn resample [N values weights]
  (let [dist (anglican.runtime/categorical (map list values weights))]
    (repeatedly N #(anglican.runtime/sample* dist))))

(def ...complete-this... nil)
;; @@

;; **
;;; In this exercise, we will define a few simple generative models, and explore different approaches to estimate the _posterior distribution_ of the latent random variables in the model.
;;; 
;;; **Reminder 1**: sum rule and product rule. Given two random variables @@x\_1, x\_2@@, there are two basic rules of probability which you need to know.
;;; 
;;; * Sum rule: marginal distributions of random variables can be found from their joint distribution, by summing (or integrating) over all other random variables, with
;;; \begin{align}
;;; p(x\_1) &= \int p(x\_1, x\_2) \mathrm{d}x\_2, & p(x\_2) &= \int p(x\_1, x\_2) \mathrm{d}x\_1
;;; \end{align}
;;; 
;;; * Product rule: the joint distribution of two random variables can be written as the product of the marginal distribution of the first random variable, times the conditional distribution of the second random variable given the first
;;; \begin{align}
;;; p(x\_1, x\_2) = p(x\_1)p(x\_2|x\_1) = p(x\_2)p(x\_1|x\_2)
;;; \end{align}
;;; 
;;; 
;;; 
;;; **Reminder 2**: a probabilistic generative model specifies a _joint distribution_ @@p(x, y)@@ over latent random variables @@x@@ and observed random variables @@y@@. A model of this sort is typically specified as
;;; 
;;; * a _prior distribution_ over the latent variables, @@p(x)@@
;;; * a _likelihood_ describing the conditional distribution @@p(y|x)@@; that is, the distribution of the data @@y@@ given the latent variables @@x@@.
;;; 
;;; The prior distribution characterizes all possible values which @@x@@ could have, for any conceivable data @@y@@.
;;; We are typically interested in the _posterior distribution_, @@p(x|y)@@, which is the conditional distribution of @@x@@ given that we have seen some particular data @@y@@. This conditional distribution is found using _Bayes' rule_, which is a consequence of the sum rule and the product rule above:
;;; \begin{align}
;;; p(x | y) = \frac{p(y|x)p(x)}{p(y)}
;;; \end{align}
;;; 
;;; The generative model specifies the joint distribution @@p(x, y)@@ which is the numerator on the right hand side of this equation. The denominator, @@p(y)@@, is unknown. However, if we are considering the posterior distribution @@p(x|y)@@, then this is a probability distribution over @@x@@, meaning we must have
;;; 
;;; $$\int p(x|y) \mathrm{d}x = 1.$$
;;; 
;;; Then, we can think of the unknown @@p(y)@@ as an unknown normalizing constant for an unnormalized @@p(x|y)@@.
;;; 
;; **

;; **
;;; # Exercise 1: Inference in a conjugate Gaussian model
;; **

;; **
;;; In our first exercise, suppose we have a very simple generative model for data:
;;; $$\begin{align}
;;; \sigma^2 &= 2 \\\\
;;; \mu &\sim \mathrm{Normal}(1, \sqrt 5) \\\\
;;; y\_i|\mu &\sim \mathrm{Normal}(\mu, \sigma).
;;; \end{align}$$
;;; 
;;; In this generative model, we have a Gaussian distribution as our prior for the latent variable @@\mu@@; the likelihood &mdash; that is, the probability of the data given the latent variables &mdash; is then also Gaussian, with mean @@\mu@@.
;; **

;; **
;;; Now suppose we observe two data points, @@y\_1 = 9@@ and @@y\_2 = 8@@.  
;;; 
;;; Reminder: the functional form of a Gaussian probability distribution over a random variable @@x@@ with mean @@\mu@@ and standard deviation @@\sigma@@ is 
;;; 
;;; $$
;;; p(x | \mu, \sigma) = 
;;; \frac{1}{\sigma \sqrt{2\pi}}
;;; \exp \left\\{ \frac{1}{2\sigma^2} (x - \mu)^2 \right\\}$$
;;; 
;;; **QUESTION 1** Write down the functional form for the joint distribution @@p(\mu, y\_1, y\_2)@@, as a product of the prior distribution @@p(\mu)@@, the likelihood @@p(y\_1,y\_2|\mu)@@
;; **

;; **
;;; 
;; **

;; **
;;; **QUESTION 2** The joint density function above, defined over @@y\_1, y\_2, \mu@@ is an unnormalized density in @@\mu@@. Normalize this density to find the posterior distribution of @@\mu | y\_1, y\_2@@ analytically.
;;; 
;;; Note that @@p(y\_1, y\_2) = \int p(y\_1, y\_2, \mu) \mathrm{d}\mu@@.
;;; 
;;; _HINT_: First, notice that the posterior distribution over @@\mu@@ is also a Gaussian distribution. Then, find the mean and variance of that Gaussian. Further hint: see http://www.cs.ubc.ca/~murphyk/Papers/bayesGauss.pdf .
;;; 
;;; 
;;; 
;; **

;; **
;;; 
;; **

;; **
;;; ### Monte Carlo inference
;;; 
;;; Only in very rare cases can we perform the integration above analytically! Normally, we will have to use some sort of algorithm to estimate the posterior distribution numerically.
;;; 
;;; A very general and widely applicable approach is based on _Monte Carlo_ methods, in which we approximate a distribution with a finite set of samples. We will look at two different approaches here.
;;; 
;;; The basic idea is as follows: suppose we want to compute expectations of some function @@f(x)@@ with respect to some distribution @@p(x | y)@@. These expectations look like
;;; $$
;;; \mathbb{E}[f(x)] = \int f(x) p(x | y) \mathrm{d}x.
;;; $$
;;; Suppose we don't know how to perform that integral, but that we _do_ have some procedure for drawing samples from the distribution @@p(x | y)@@. Then we can approximate the integral with a finite sum:
;;; \begin{align}
;;; \mathbb{E}[f(x)] &\approx \sum\_{i=1}^N f(x_i) &
;;; \text{where each } x\_i &\sim p(x | y).
;;; \end{align}
;;; As the number of samples @@N \rightarrow \infty@@, this approximation becomes exact due to the Law of Large Numbers.
;;; 
;;; Next question: how do we draw samples from some arbitrary (possibly unnormalized) distribution @@p(x | y)@@?
;;; 
;; **

;; **
;;; #### Method 1: likelihood weighting
;;; 
;;; Our first inference algorithm will sidestep the need for drawing samples from @@p(x | y)@@ by instead drawing samples from some other _proposal distribution_ @@q(x)@@, which we do know how to sample from. We can rewrite the expectation as
;;; 
;;; $$
;;; \mathbb{E}[f(x)] = \int f(x) p(x | y) \mathrm{d}x
;;;  = \int f(x) p(x | y) \frac{q(x)}{q(x)} \mathrm{d}x
;;;  = \int f(x) W(x) q(x) \mathrm{dx}
;;;  = \mathbb{E}_q[f(x)W(x)]
;;; $$
;;; 
;;; where @@W(x) = p(x|y)/q(x)@@. The idea here is we can now approximate this expectation with _weighted samples_ from @@q(x)@@.
;;; 
;;; Algorithmically, this works as follows: we define an unnormalized _importance weight_ function
;;; $$w(x) = \frac{p(x,y)}{q(x)}.$$
;;; We then draw samples @@x\_i \sim q(x)@@ for @@i = 1,\dots, N@@ and approximate expectations with
;;; 
;;; $$W\_i = \frac{w(x\_i)}{\sum_{j=1}^N w(x\_j)}$$
;;; 
;;; $$\mathbb{E}[f(x)] \approx \sum^N\_{i=1} W\_i f(x\_i)$$
;;; 
;; **

;; **
;;; **OPTIONAL MATH EXERCISE** Show that the average of the unnormalized importance weights is an unbiased estimate of the evidence (i.e. of the normalizing constant for the posterior distribution): that is, show that
;;; 
;;; $$\mathbb{E}\left[\frac{1}{N} \sum\_{i=1}^N w(x\_i)\right] = p(y)$$
;; **

;; **
;;; For the Gaussian model example, a simple choice of proposal is just the prior distribution, 
;;; $$q(\mu) = \mathrm{Normal}(\mu | 1, \sqrt{5}).$$
;;; 
;;; **EXERCISE** In the Gaussian model above, if we propose from the prior distribution for @@q(\mu) = p(\mu)@@, what are the importance weights?
;; **

;; **
;;; 
;; **

;; **
;;; We provide two helper functions for working with Gaussians: `(randn mu sigma)` will sample a random variable, and `(normal-logpdf x mu sigma)` will compute @@p(x  | \mu, \sigma)@@.
;; **

;; @@
(randn 0 1)

(normal-logpdf 1.5 1 2)
;; @@

;; **
;;; **PROGRAMMING EXERCISE** Write code to use this importance sampling algorithm to compute the expected value of @@x@@ and @@x^2@@.
;;; 
;;; You should fill in two functions: `sample-from-proposal`, which draws a candidate value of `x`, and `compute-unnormalized-log-weight`, which computes @@\log p(y_1, y_2 | \mu)@@.
;;; 
;;; Before writing these functions, look ahead to the next cell, which shows how these samples and unnormalized weights will be used.
;; **

;; @@
(def N 1000)

(def dataset [9 8])

(defn sample-from-proposal
  "sample from a proposal distribution q(mu) = p(mu)"
  []
  ...complete-this...)
  
(defn compute-unnormalized-log-weight 
  "return the log likelihood of the data, given a mean mu:
  
   log p(y1, y2| mu)"
  [mu]
  ...complete-this...)
;; @@

;; @@
; Draw N proposal samples
(def proposal-samples (repeatedly N sample-from-proposal))

; Compute the unnormalized weights for each proposal sample
(def unnormalized-log-weights
  (map compute-unnormalized-log-weight proposal-samples))

; Normalize the unnormalized log weights
(defn normalize [log-w]
  (let [denominator (Math/log (reduce + (map #(Math/exp %) log-w)))]
    (map #(Math/exp (- % denominator)) log-w)))

(def normalized-weights (normalize unnormalized-log-weights))
;; @@

;; @@
(take 10 normalized-weights)
;; @@

;; **
;;; **PROGRAMMING EXERCISE** Recalling the formula for variance of @@\mathbb{V}[x] = \mathbb{E}[x^2] - \mathbb{E}[x]^2@@, use the `proposal-samples` and `normalized-weights` to estimate @@\mu@@ and @@\sigma@@.
;; **

;; @@
(def hat-x ...complete-this...)
(def hat-x-squared ...complete-this...)

(println "estimate of mu:" hat-x)
(println "estimate of sigma:" (Math/sqrt (- hat-x-squared (* hat-x hat-x))))
;; @@

;; **
;;; **EXERCISE** How do these compare with the values you computed analytically above? How stable are these estimates if you re-run the Monte Carlo algorithm several times? How do the quality of the results change as you change @@N@@?
;; **

;; **
;;; If we plot the proposal samples (ignoring the weights;;
;;;  in blue), we have a set of samples from the prior distribution. If we resample these values according to their weights, we can plot an approximate posterior histogram (in green).
;; **

;; @@
(plot/compose
  (plot/histogram proposal-samples :normalize :probability-density :plot-range [[-5 9] [0 0.5]])
  (plot/histogram (resample N proposal-samples normalized-weights) :normalize :probability-density :color "#33AA11"))
;; @@

;; **
;;; **EXERCISE** Look at the weight vector itself, and (qualitatively) describe it. If we drew @@N@@ samples originally, what would you say the "effective sample size" is for the weighted sample estimate of the posterior?
;; **

;; @@
(plot/list-plot (map vector proposal-samples normalized-weights))
;; @@

;; **
;;; # Exercise 2: The seven scientists
;;; 
;;; Here's an interesting variation on estimating the mean of a Gaussian. This example is due to [MacKay 2003, exercise 22.15] and [Lee & Wagenmaker 2013, section 4.2].
;;; 
;;; Suppose seven scientists all go and perform the same experiment, each collecting a measurement @@x\_i@@ for @@i = 1,\dots,7@@. 
;;; 
;;; These scientists are varyingly good at their job, and while we can assume each scientist would estimate @@x@@ correctly _on average_, some of them may have much more error in their measurements than others.
;;; 
;;; They come back with the following seven observations:
;; **

;; @@
(def measurements [-27.020 3.570 8.191 9.898 9.603 9.945 10.056])

(plot/bar-chart (range 1 8) measurements)
;; @@

;; **
;;; Clearly scientist 1 does not know what he is doing (and 2 and 3 are probably a little suspect too)!
;;; 
;;; To model this situation, we place simple priors on the mean @@\mu@@ of the measurements, and the error standard deviation @@\sigma\_i@@ for each of the @@i@@ scientists.
;;; 
;;; As a starting point, consider placing uninformative priors on these parameters; a suggestion is
;;; $$\begin{align}
;;; \mu &\sim \mathrm{Normal}(0, 50) \\\\
;;; \sigma\_i &\sim \mathrm{Uniform}(0, 25)
;;; \end{align}$$
;;; 
;;; We then suppose each data point is distributed with the same mean, but with a scientist-specific standard deviation:
;;; \begin{align}
;;; y\_i &\sim \mathrm{Normal}(\mu, \sigma_i)
;;; \end{align}
;;; 
;;; We can ask two questions, here:
;;; 
;;; * Given these measurements, what is the posterior distribution of @@x@@?
;;; * What distribution over noise level @@\sigma_i@@ do we infer for each of these scientists' estimates?
;;; 
;; **

;; **
;;; ### Inference Method 2: Markov chain Monte Carlo
;;; 
;;; In the previous exercise, we used two inference methods: exact analytic integration, and likelihood weighting. There are two problems:
;;; 
;;; * Exact analytic integration is only possible for very particular special cases, and we cannot perform this here
;;; * Likelihood weighting degrades poorly as the dimension of the latent variables increases, unless we have a very well-chosen proposal distribution @@q(x)@@.
;;; 
;;; For this exercise we will use a different approach, Markov chain Monte Carlo (MCMC). MCMC methods draw samples from a target distribution by performing a biased random walk over the space of the latent variables @@x@@. Technically, this works by constructing a Markov chain whose stationary distribution is the target distribution we are trying to sample from. For the moment do not worry about _why_ MCMC works; first, here is how to implement it algorithmically.
;;; 
;;; MCMC also uses a proposal distribution, but this proposal distribution makes _local_ changes to the latent variables @@x@@. This proposal @@q(x' | x)@@ defines a conditional distribution over @@x'@@ given a current value @@x@@.
;;; There is a lot of freedom in choosing different sorts of creative proposal distributions, but a simple and typical class of proposal distributions for real-valued latent variables takes a value @@x@@ and adds a small amount of Gaussian noise along one or more of its dimensions.
;;; 
;;; Assuming we are trying to sample from a posterior distribution @@p(x | y) \propto p(x, y)@@, we define an _acceptance ratio_
;;; \begin{align}
;;; A(x \rightarrow x') &= {\rm min} \left(1, ~\frac{p(x', y)q(x | x')}{p(x, y)q(x' | x)} \right)
;;; \end{align}
;;; After we propose some new value @@x'@@, we then _accept_ it with probability @@A(x \rightarrow x')@@ and "move" to the new position @@x'@@, otherwise we _reject_ it and stay at @@x@@.
;;; 
;;; This entire sequence of values at every iteration (including the duplicated values after a reject step) are jointly a sample from the posterior distribution @@p(x | y)@@.
;;; 
;;; If we choose a proposal distribution @@q(x' | x)@@ that is symmetric, such that @@q(x' | x) = q(x | x')@@, then the acceptance ratio simplifies to a ratio of the joint distributions using the new and old values of @@x@@. A simple intuitive interpretation of the algorithm in this case is as a sort of noisy hill-climbing on @@p(x, y)@@; "better" values of @@x'@@ are accepted always, and "worse" values of @@x'@@ are accepted "sometimes".
;;; 
;; **

;; **
;;; **PROGRAMMING EXERCISE** Write an MCMC sampler for drawing samples from the posterior distribution over @@\mu@@ and each @@\sigma_i@@, given the observed data.
;;; 
;;; For purposes of our exercise, each sampled value will be a map with two keys, `:mu` and `:sigma`; these have as values, repectively, a single number, and a vector of 7 numbers.
;;; 
;;; Here is an example, which we will use as an initial value for our MCMC sampler:
;; **

;; @@
(def initial-value {:mu 0.0
                    :sigma [10. 10. 10. 10. 10. 10. 10.]})
;; @@

;; **
;;; Step 1: Define an expression for the log-joint over all latent and observed random variables.
;; **

;; @@
(defn uniform-logp 
  "Helper function: log-probability of uniform distribution on [0, 25]"
  [value]
  (if (and (>= 25 value) (>= value 0))
    (Math/log (/ 1. 25))
    (Math/log 0.)))


(defn log-joint 
  "Compute the joint log-probability of all random variables:
  
   log p(mu, sigma_1, ..., sigm_7, y_1, ... y_7)
  
   The input `value` has the form {:mu float :sigma [float ... float]}"
  [value]
  ...complete-this...)
;; @@

;; **
;;; Step 2: Define a proposal distribution. 
;;; 
;;; A good choice of proposal distribution is to add a small amount of Gaussian noise @@\epsilon_{\sigma}@@ to each 
;;; @@\sigma\_i@@, 
;;; 
;;; and @@\epsilon\_\mu@@ to @@\mu@@.
;; **

;; @@
(def epsilon-mu 0.5)
(def epsilon-sigma 0.25)

(defn propose [prev-value]
  ...complete-this...)
;; @@

;; **
;;; Step 3: Define the accepance ratio for the MCMC sampler.
;; **

;; @@
(defn acceptance-ratio [prev-value new-value]
  ...complete-this...)
;; @@

;; **
;;; Step 4: Draw @@N@@ samples, by repeatedly applying the transition operation.
;; **

;; @@
(def N 10000)

; This should contain a list or vector of N samples
(def MCMC-samples
  
  ...complete-this...)

;; @@

;; **
;;; Now, let's look at our samples. What happens as we run the sampler? Let's plot the estimate of our mean:
;; **

;; @@
(plot/list-plot (map :mu MCMC-samples) :joined true)
;; @@

;; **
;;; The horizontal axis here is _iteration of the sampling algorithm_.
;;; 
;;; The sampler starts off with a random-walk behaviour, so one typically must disregard the early values drawn from an MCMC sampler. This period, before the sampler is actually drawing from the correct target distribution, is known as "burnin". We'll plot histograms for all our quantities, disregarding the first 50% of samples.
;; **

;; @@
(def burnin (int (/ N 2)))
;; @@

;; @@
(println "Expected value of measured quantity:" (mean (map :mu (drop burnin MCMC-samples))))

(plot/histogram (map :mu (drop burnin MCMC-samples))
                :normalize :probability
                :bins 20)
;; @@

;; @@
(def noise-estimate (mean (map :sigma (drop burnin MCMC-samples))))

(plot/bar-chart (range 1 8) noise-estimate)
;; @@

;; **
;;; **QUESTIONS**
;;; 
;;; * Are these noise levels what you would expect?
;;; * How sensitive is this to the prior on @@\mu@@ and @@\sigma\_i@@?
;;; * How sensitive are these results to the choices of @@\epsilon@@ in our sampler?
;;; * Plot the convergence (trace plot) for the @@\sigma_i@@ like we did above for @@mu@@. What can you say about our posterior uncertainty on the measurement error?
;; **

;; **
;;; 
;; **
