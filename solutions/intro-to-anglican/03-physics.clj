;; gorilla-repl.fileformat = 1

;; **
;;; 
;; **

;; **
;;; 2D Physics
;;; ==========
;;; 
;;; Probabilistic programming inference through a complex nondifferentiable deterministic procedure, here a 2D physics engine; using inference to do mechanism design, namely positioning bumpers to safely deliver a number of bouncy balls into a waiting bin.
;;; 
;;; Authors:
;;; 
;;;  - Frank Wood [fwood@robots.ox.ac.uk](mailto:fwood@robots.ox.ac.uk)
;;;  - Brooks Paige [brooks@robots.ox.ac.uk](mailto:brooks@robots.ox.ac.uk)
;;; 
;;; Task
;;; ====
;;; 
;;; Run the code below to see a 2D "world" simulation.  Then write (modify) a query to infer bumper locations that get, ideally, all of the ten dropped balls into the bin.
;;; 
;;; The world simulation code itself is writen in a separate file, `src/bounce.clj`, which we load here. You don't need to look at the simulation code itself to perform the exercise, unless you are interested.
;; **

;; @@
(ns physics
  (:require [org.nfrac.cljbox2d.core :refer [position]])
  (:use [anglican core runtime emit]))

(require '[exercises.physics 
           :refer [create-world show-world-simulation 
                   simulate-world display-static-world
                   balls-in-box]] :reload)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; The task: a Rube-Goldberg device design task of sorts; given a ball emitter and a bin into which you would like the balls to end up, use inference to configure a set of bumpers such that the balls do end up in the bin.
;;; 
;;; Here's a purposefully bad guess as to where bumpers might go; the values used should also give you a sense of the dimensions of the world (for ease of reference, the bin is at [15 0], is 2 units wide and has walls of height 2, balls are dropped from around [-5 8.25] down a ramp that goes from [-6 9] to [-4 7]).
;;; 
;;; The bumper locations themselves are a list of pairs, where the pairs are the center of the bumpers.
;; **

;; @@
;; A bumper location example is a list of pairs.
;; Here as an example, we place 4 different bumpers:
(def bumper-location-example 
  [[-3 6] [2 5] [7 4] [12 3]])

;; Create a world with given bumpers at these example locations:
(def example-world (create-world bumper-location-example))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;physics/example-world</span>","value":"#'physics/example-world"}
;; <=

;; **
;;; There are two ways of examining the "world" we just created. The most exciting is to play forward the balls bouncing, in real time, and see where they go! For this, we use the `show-world-simulation` function we provide.
;;; 
;;; When you run the following line, it will open up a Java applet window showing the simulation in progress. Note that the window might not open up "on top" of this browser window.
;;; 
;;; Also, note that every time you run this, a _new_ applet window will be created.
;; **

;; @@
;; and to see why you can run the simulator in "real time"
;; watching it as it goes
(show-world-simulation bumper-location-example)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>#object[quil.Applet 0x74c91783 &quot;quil.Applet[panel0,0,0,600x500,layout=java.awt.FlowLayout]&quot;]</span>","value":"#object[quil.Applet 0x74c91783 \"quil.Applet[panel0,0,0,600x500,layout=java.awt.FlowLayout]\"]"}
;; <=

;; **
;;; We don't need to simulate in real time -- here, the `simulate-world` function runs the simulation forward, dropping 10 balls, and recording their locations after 20 seconds.
;;; 
;;; We can look at their locations, and see where they end up (visually, `display-static-world` can be used for this). For the (provided) example locations, none of the balls end in the bucket.
;; **

;; @@
;; Simulate the final state using the 2d physics engine:
(def example-world-final-state (simulate-world example-world))

;; The ball positions can be examined by getting `:balls` from the final state:
(map position (:balls example-world-final-state))

;; Show the conclusion of simulation -- note
;; that you see no balls which suggests they have missed
;; the target:
(display-static-world bumper-location-example
                      (:balls example-world-final-state))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>#object[quil.Applet 0x3c6515bc &quot;quil.Applet[panel1,0,0,600x500,layout=java.awt.FlowLayout]&quot;]</span>","value":"#object[quil.Applet 0x3c6515bc \"quil.Applet[panel1,0,0,600x500,layout=java.awt.FlowLayout]\"]"}
;; <=

;; **
;;; ## Inference
;;; 
;;; We provide a useful function (defined in `src/exercises/physics.clj`) which counts the number of balls which are in the box:
;; **

;; @@
;; Just to check, our guess at the bumper locations
;; leaves none in the box:
(balls-in-box example-world-final-state)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"}
;; <=

;; **
;;; Instead of painstakingly hand-tuning the bumper locations, let's use inference to figure out where to place them. Here we wish to define a probabilistic model that assigns higher probability to executions in which the desired number of balls end up in the bucket.
;;; 
;;; To do this, we need to 
;;; 
;;; 1. define simulation code (aka, a "prior") which defines a distribution over the number of bumpers and their locations, and
;;; 2. define a likelihood (using `observe`) to penalize program executions (i.e. configurations of bumpers) which do not place balls in the bucket.
;;; 
;;; A skeleton of the query is here below:
;; **

;; @@
(def start-x -5)
(def start-y 8.25)
(def bin-x 15)
(def bin-y 0)

;; To do this we've provided scaffolding
;; that can be modified to achieve
;; your objective:
(with-primitive-procedures
  [create-world simulate-world balls-in-box]
  (defquery arrange-bumpers []
    (let [bumper-positions 
          (repeatedly 10
                      (fn []
                        (let [x (sample (uniform-continuous start-x bin-x))
                              y (sample (uniform-continuous bin-y start-y))]
                          [x y])))
          ;; Code to simulate the world:
          world (create-world bumper-positions)
          end-world (simulate-world world)
          balls (:balls end-world)

          ;; How many balls entered the box?
          num-balls-in-box (balls-in-box end-world)]
      (observe (exponential 1.0) 
               (- 20 num-balls-in-box))
      {:balls balls
       :num-balls-in-box num-balls-in-box
       :bumper-positions bumper-positions})))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;physics/arrange-bumpers</span>","value":"#'physics/arrange-bumpers"}
;; <=

;; **
;;; Here is some basic inference code; we run lightweight metropolis-hastings for 1500 samples, and then take the last one. This should yield a sample which does reasonably well at the task:
;; **

;; @@
(def samples (map :result (doquery :rmh arrange-bumpers nil)))

;; We will use a silly notion of what is "best," namely
;; the configuration learned after 1500 sweeps, i.e.
;; single variable proposed changes, effectively
;; counting on lmh to stochastically ascend the 
;; posterior but not mix (a reasonably safe bet here):
(def best-configuration (first (drop 1500 samples)))

;; We can look at the best configuration:
best-configuration

;; And look at convergence with respect to the
;; number of balls left in the box:
(->> (take 1500 samples)
     (take-nth 10)
     (map :num-balls-in-box))
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"}],"value":"(0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10)"}
;; <=

;; **
;;; Now, we can watch the simulation in action with our inferred world:
;; **

;; @@
;; This is what it looks like when run with 20 balls:
(show-world-simulation (:bumper-positions best-configuration))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>#object[quil.Applet 0x5da5e3f4 &quot;quil.Applet[panel2,0,0,600x500,layout=java.awt.FlowLayout]&quot;]</span>","value":"#object[quil.Applet 0x5da5e3f4 \"quil.Applet[panel2,0,0,600x500,layout=java.awt.FlowLayout]\"]"}
;; <=

;; **
;;; Good luck!
;;; ==========
;; **
