;; gorilla-repl.fileformat = 1

;; **
;;; # Exercise 1: An Anglican Probabilistic Hello World
;;; 
;;; This file is a [Gorilla Repl](http://gorilla-repl.org/index.html) worksheet. This is a notebook format which allows writing Clojure (and Anglican) code in cells within a document. Conceptually this is quite similar to (e.g.) iPython notebooks.
;;; 
;;; Shift + enter evaluates a code block. Hit ctrl+g twice in quick succession or click the menu icon (upper-right corner) for more commands.
;; **

;; **
;;; This worksheet, written by
;;; 
;;; - Frank Wood : [fwood@robots.ox.ac.uk](mailto:fwood@robots.ox.ac.uk)
;;; - Brooks Paige : [brooks@robots.ox.ac.uk](mailto:brooks@robots.ox.ac.uk)
;;; - Jan-Willem van de Meent : [jwvdm@robots.ox.ac.uk](mailto:jwvdm@robots.ox.ac.uk)
;;; 
;;; is designed to introduce you to the basics of Clojure and Anglican.
;;; It introduces probabilistic programming inference in a "Hello World!" Beta-Bernoulli model, illustrates different ways of writing queries, and shows different ways of consuming their output.
;;; 
;;; 
;;; The following cell defines a _namespace_, and imports some functions we will need. This is a Clojure concept somewhat analogous to a class in Java, or a module in Python. For now, take this as given; we will supply necessary imports at the top of the document for all the examples.
;;; 
;;; Run it by clicking on it and hitting shift+enter. This block of code often takes 10 seconds or more to run while Clojure unpacks and initializes dependencies.
;;; 
;;; Output will appear just below the cell; in this case we expect `nil`.
;; **

;; @@
(ns hello-world
  (:require [gorilla-plot.core :as plot])
  (:use [anglican core runtime emit stat]))
;; @@

;; **
;;; ## Anglican overview
;;; 
;;; The Anglican system consists of three main components:
;;; 
;;; 1. A [language](http://www.robots.ox.ac.uk/~fwood/anglican/language/index.html) for defining probabilistic programs. This language implements a large subset of the language features in Clojure. We refer to an Anglican program as a *query*.
;;; 
;;; 2. An inference backend that implements a number of different [inference methods](http://www.robots.ox.ac.uk/~fwood/anglican/inference/index.html) for Anglican queries. 
;;; 
;;; 3. A [library](https://crossclj.info/ns/anglican/1.0.0/anglican.runtime.html) of functions such as basic math operations and constructors for *distribution* objects, all of which can be used both in Anglican and in Clojure programs.
;;; 
;;; 
;;; ## Distributions 
;;; 
;;; Anglican provides a number of distriubtion primitives to the language, for example `normal`. Calling `(normal mu std)`, with arguments `mu` and `std`, creates a _distribution object_. A distribution object can be used both in Clojure programs and in Anglican programs, but behaves a little differently in each case.
;;; 
;;; In a Clojure program, a distribution object implements two methods
;;; 
;;; 1. `sample*` generates a sample from the distribution. For example `(sample* (normal 0.0 1.0))` draws a standard normal random variate.
;;; 
;;; 2. `observe*` computes the log probability of a sample. For example `(observe (normal 0.0 1.0) 3.0)` returns the log probability of the value `3.0` under the distribution `(normal 0.0 1.0)`.
;;; 
;;; In an Anglican program, there are two *special forms* that interact with distribution objects
;;; 
;;; 1. `sample` asks the inference backend to generate a sample from the distribution. By default the backend does this by simply calling the Clojure function `sample*`. However in some inference algorithms the backend may resuse a previously sampled value, or sample from a learned proposal.
;;; 
;;; 2. `observe` askes the inference backend to update the log probability of the current execution according the the log probability that can be calculated using the Clojure function `observe*`.
;;; 
;;; Below are some example distribution primitives; these are sufficient to solve the exercises.  A full list of built-in primitives can be found [here](http://www.robots.ox.ac.uk/~fwood/anglican/language/index.html).
;; **

;; @@
;; Draw from a normal distribution with mean 1 and standard deviation 2:
(sample* (normal 1 2))

;; Flip a coin, which comes up `true` with probability 0.7, and false with probabilty 0.3:
(sample* (flip 0.7))

;; Sample from a uniform distribution on the open interval (3, 10):
(sample* (uniform-continuous 3 10))

;; Sample from a beta distribution with parameters a=2, b=3:
(sample* (beta 2 3))

;; Sample from a binomial distribution with n=10 and p=0.4:
(sample* (binomial 10 0.4))

;; Sample from a discrete distribution with probabilities [0.3 0.2 0.5] on 0, 1, 2:
(sample* (discrete [0.3 0.2 0.5]))
;; @@

;; @@
;; `repeatedly` can be pretty useful, here.
;; Suppose we want to draw 10 samples from the same normal distribution:
(let [normal-dist (normal 1 2.2)]
  (repeatedly 10 (fn [] (sample* normal-dist))))

;; The # symbol can be used as a shorthand for function definition.
;; The same code as the previous line can also be written like so:
(let [normal-dist (normal 1 2.2)]
  (repeatedly 10 #(sample* normal-dist)))
;; @@

;; @@
;; Using observe: log p(x=3), where x ~ Normal(0, 1):
(observe* (normal 0 1) 3)
;; @@

;; **
;;; ## A First Anglican Query
;; **

;; **
;;; Let's use Anglican (and what we've learned) to pose a simple statistical query under the model
;;; 
;;; $$\begin{align}\theta &\sim \mathrm{Beta}(5,3) \\\\
;;; y &\sim \mathrm{Bernoulli}(\theta)\end{align}$$
;;; 
;;; and ask 
;;; 
;;; $$p(\theta>0.7 | y = true).$$
;;; 
;;; For this we can easily look up and/or compute the ground truth = 
;;; 
;;; $$p(\theta>0.7 | y = true) = .448 = 1-\mathrm{betacdf}(6,3,0.7)$$
;;; 
;;; Probabilistic models written in Anglican are called `queries`, and are defined using `defquery`.
;;; 
;;; The following program defines the statistical model above:
;; **

;; @@
(defquery one-flip [y]
  (let [theta (sample (beta 5 3))]
    (observe (flip theta) y)
    (> theta 0.7)))
;; @@

;; **
;;; Take a moment to make sure that code block makes sense! `defquery` looks a lot like a function definition, except the contents of the `defquery` are actually Anglican code, which is then _compiled_ into a computable representation of the posterior (think sampler).
;;; 
;;; - The query is named `one-flip`, and it takes a single argument `y`, which is the observed value.
;;; 
;;; - The `let` block defines `theta` as a random sample from the distribution `(beta 5 3)`.
;;; 
;;; - The `observe` statement asserts that we see `y` as data generated from `(flip theta)`.
;;; 
;;; - The final statement defines the return value for the program, which is equal to the `true/false` value of the expression `(> theta 0.7)`.
;;; 
;;; Together, these four lines define our first Anglican program/query/model.
;; **

;; **
;;; ## Sampling from the posterior
;;; 
;;; 
;; **

;; **
;;; The `doquery` command can be used to perform inference on a query with a specified algorithm
;; **

;; @@
(def rmh-samples
  (take 1000
        (doquery :rmh one-flip [true])))
;; @@

;; **
;;; The `doquery` command defines an infinite lazy sequence, of which we grab 1000 samples.  **Important**: Do not try to print out or directly evaluate the output from a `doquery` call – this will try to print out an infinite sequence.
;;; 
;;; We here use the `:rmh` algorithm to perform inference. Inference algorithms can accept various optional arguments which are used to specify the algorithm used for posterior sampling. Sensible values for these are provided in all exercises, and all different options are described in the [inference algorithms documentation](http://www.robots.ox.ac.uk/~fwood/anglican/language/index.html).
;; **

;; **
;;; Each sample returned by `doquery` is a hashmap with three entries
;; **

;; @@
(first rmh-samples)
;; @@

;; **
;;; The `:result` entry contains the return value for each program execution. The `:log-weight` entry contains the corresponding log probability. The `:predict` key stores the output of `(predict ...)` commands, which we ill get back to later in this workbook. 
;;; 
;;; Now that you have generated your first set of samples, you can now get a posterior estimate by extracting the `:result` value from each sample, e.g.  
;; **

;; @@
(frequencies
  (map :result
       rmh-samples))
;; @@

;; **
;;; here, `frequencies` is a standard Clojure function that counts the number of occurences of each unique element in a sequence. 
;; **

;; **
;;; ## Importance Sampling
;; **

;; **
;;; Anglican provides different classes of inference methods. MCMC methods, such as `:rmh` return unweighted samples, whereas importance sampling methods, such as `:importance` and `:smc` return weighted samples:
;; **

;; @@
(def rmh-samples
  (take 1000
	    (doquery :rmh one-flip [true])))

(def importance-samples
  (take 1000
	    (doquery :importance one-flip [true])))

(first rmh-samples)
(first importance-samples)
;; @@

;; **
;;; If you naively average over weighted samples, you will get incorrect results
;; **

;; @@
(frequencies
  (map :result
       rmh-samples))

(frequencies
  (map :result
       importance-samples))
;; @@

;; **
;;; Anglican provides a function known as `collect-results` which summarizes a set of weighted samples
;; **

;; @@
(collect-results 
  (take 10000 importance-samples))
;; @@

;; **
;;; The output of `collect-results` is a map `{value log-weight}`, which can be post-processed using functions in `anglican.stat`. For example we use `empirical-distribution` to normalize log weights into probabilities that sum to 1.0:
;; **

;; @@
(empirical-distribution
  (collect-results 
    rmh-samples))

(empirical-distribution
  (collect-results 
    importance-samples))
;; @@

;; **
;;; Finally, you can use the `(predict ...)` special form to define additional outputs in the program. Unlike the `:result` value, these outputs do not have to be define at the end of the program. For example:
;; **

;; @@
(defquery many-flips [y-values]
  (let [theta (sample (beta 5 3))
        outcome-dist (flip theta)]
    (predict :theta theta)
    (map (fn [y] 
           (observe outcome-dist y)) 
         y-values)
    (> theta 0.7)))
;; @@

;; **
;;; You will now see an entry under `:predicts` in the returned samples
;; **

;; @@
(first 
  (doquery :importance many-flips 
           [[true true true]]))
;; @@

;; **
;;; Anglican provides a function called `get-predicts`, in `anglican.state`, which turns the predicts as a hashmap
;; **

;; @@
(require '[anglican.state :refer [get-predicts]])

(get-predicts
  (first 
    (doquery :importance 
             many-flips [[true true true]])))
;; @@

;; **
;;; ## A Second Query: Multiple Observes
;;; 
;;; How would we modify this model to return, instead of a one-flip posterior, the posterior distribution given a sequence of flips? That is, we keep the basic model
;;; 
;;; $$\begin{align}\theta &\sim \mathrm{Beta}(5,3) \\\\
;;; y\_i &\sim \mathrm{Bernoulli}(\theta)\end{align}$$
;;; 
;;; and ask 
;;; 
;;; $$p(\theta>0.7 | x\_i)$$
;;; 
;;; for some sequence @@x\_i@@. Now, we let `outcomes`, the argument to our query, be a sequence, and we can use `map` (or `loop` and `recur`) to `observe` all different outcomes.
;;; 
;;; Here's one way of writing this:
;; **

;; @@
(defquery many-flips [y-values]
  (let [theta (sample (beta 5 3))
        outcome-dist (flip theta)]
    (map (fn [y] 
           (observe outcome-dist y)) 
         y-values)
    (> theta 0.7)))
;; @@

;; **
;;; We can use `doquery` to estimate the posterior distribution of @@\theta > 0.7@@ given the sequence `[true, false, false, true]`, just as before (the analytical answer is 0.21).
;; **

;; @@
(def data [true, false, false, true])

(def rmh-samples 
  (take 1000 (doquery :rmh many-flips [data])))

(frequencies
  (map :result rmh-samples))
;; @@

;; **
;;; ## Visualizing Results
;; **

;; **
;;; A rudimentary plotting capability comes as part of [Gorilla REPL](http://gorilla-repl.org/).  Here we use a histogram plot to show the posterior distribution on `theta`.
;; **

;; **
;;; 
;; **

;; @@
(defquery many-flips [y-values]
  (let [theta (sample (beta 5 3))
        outcome-dist (flip theta)]
    (map (fn [y] 
           (observe outcome-dist y)) 
         y-values)
    theta))

(def rmh-samples 
  (take 1000 (doquery :rmh many-flips [data])))

(plot/histogram 
  (map :result rmh-samples)
  :bins 20 :normalize :probability)
;; @@

;; **
;;; That's it! Now move onto the exercises, or take a look at some of the advanced usage examples below. Keep this worksheet open in a separate tab or window, and refer to it for language reference.
;; **

;; **
;;; ## Advanced Usage: predict
;; **

;; **
;;; Anglican allows you to define additional program outputs using the `predict` command
;; **

;; @@
(defquery many-flips [y-values]
  (let [theta (sample (beta 5 3))
        outcome-dist (flip theta)]
    (predict :theta theta)
    (map (fn [y] 
           (observe outcome-dist y)) 
         y-values)
    (> theta 0.7)))
;; @@

;; **
;;; The values associated with `predict` commands are stored in the `:predict`
;; **

;; @@
(def rmh-samples 
  (take 1000 (doquery :rmh many-flips [data])))

(first rmh-samples)
;; @@

;; **
;;; You can extract these values using the `get-predicts` function in `anglican.state`
;; **

;; @@
(require '[anglican.state :refer [get-predicts]])

(get-predicts (first rmh-samples))
;; @@

;; **
;;; Anglican also provides `collect-predicts`, which is the analogue of `collect-results`
;; **

;; @@
(empirical-mean
  (collect-predicts 
    :theta
    rmh-samples))

(empirical-std
  (collect-predicts 
    :theta
    rmh-samples))
;; @@

;; **
;;; ## Andvanced Usage: conditional
;;; 
;;; 
;;; The `conditional` function takes a query and returns a distribution object constructor (think of the returned object as a factory for conditional/parameterized distributions). 
;;; 
;;; The following line defines `one-flip-posterior` as a distribution constructor which will draw posterior samples from the distribution defined by our query above, using the Lightweight Metropolis-Hastings (`:lmh`) algorithm.
;; **

;; @@
(require 'anglican.rmh)
(def one-flip-posterior (conditional many-flips :rmh))
;; @@

;; **
;;; The object we just created plays the same role as `normal`, `flip`, or other built-in distribution constructors (except one can only `sample` but not `observe` from distributions created using `conditional`).
;;; 
;;; To actually create the posterior distribution itself, we create a distribution object which takes the query argument `outcome`. 
;;; 
;;; This is analogous to how when creating a normal distribution we must specify the mean and standard deviation, e.g. `(normal 0 1)`. Here, we specify whether our one outcome was true or false.
;; **

;; @@
(def true-flip-posterior (one-flip-posterior data))
;; @@

;; **
;;; Now, we can draw samples just as we would draw samples from a distribution created by calling `(normal 0 1)`. A sample from a conditional distribution defined in this way returns a key-value map, where the keys are the same as those specified in the `predict` statements.
;;; 
;;; To index into a hashmap in Clojure, just use the key as a function.
;; **

;; @@
;; Draw one sample (returns true or false):
(sample* true-flip-posterior)
;; @@

;; **
;;; Sampling repeatedly from this distribution object characterizes the distribution.	
;; **

;; @@
(frequencies (repeatedly 
               1000
               #(sample* true-flip-posterior)))
;; @@
