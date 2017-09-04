;; gorilla-repl.fileformat = 1

;; **
;;; # Exercise 2: Gaussian posterior estimation
;; **

;; @@
(ns gaussian-estimation
  (:require [gorilla-plot.core :as plot]
            [anglican.stat :as stat])
  (:use [anglican core emit runtime]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; We can use Anglican for posterior estimation in Bayesian models.
;;; 
;;; Suppose we are trying to estimate the mean of a Gaussian distribution, given some observed data @@y\_i@@.
;;; We'll assume that the variance is known, and focus on learning the posterior distribution of the mean @@\mu@@.
;;; We put a Gaussian prior on @@\mu@@, yielding a model:
;;; 
;;; $$\begin{align}
;;; \sigma^2 &= 2 \\\\
;;; \mu &\sim \mathrm{Normal}(1, \sqrt 5) \\\\
;;; y\_i|\mu &\sim \mathrm{Normal}(\mu, \sigma).
;;; \end{align}$$
;;; 
;;; Now suppose we observe two data points, @@y\_1 = 9@@ and @@y\_2 = 8@@.
;;; This will be passed as an input to the query as a vector with two elements.
;; **

;; @@
(def y1 9)
(def y2 8)
(def dataset [y1 y2])
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;gaussian-estimation/dataset</span>","value":"#'gaussian-estimation/dataset"}
;; <=

;; **
;;; Write this model as a simple Anglican program:
;; **

;; @@
(defquery gaussian-model [y1 y2]
  (let [;; define the observation noise
        sigma2 2
        ;; sample observation mean from prior
        mu (sample (normal 1.0 (sqrt 5)))
        ;; define the observation likelihood
        like (normal mu (sqrt sigma2))]
    ;; observe y1 and y2 acccording to the liklihood
    (observe like y1)
    (observe like y2)
    ;; return the observation mean
    mu))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;gaussian-estimation/gaussian-model</span>","value":"#'gaussian-estimation/gaussian-model"}
;; <=

;; **
;;; The following line now draws 20,000 samples using Metropolis-Hastings sampling
;; **

;; @@
(def posterior-samples  
  (take 10000
        (drop 10000
              (map :result
                   (doquery :rmh gaussian-model [y1 y2] 
                            :number-of-particles 1000)))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;gaussian-estimation/posterior-samples</span>","value":"#'gaussian-estimation/posterior-samples"}
;; <=

;; **
;;; We can plot a histogram of these samples to see the posterior.
;;; 
;;; Here we have chosen the [conjugate prior](http://en.wikipedia.org/wiki/Conjugate_prior) for @@\mu@@, making this a rare model in that we can actually compute the posterior distribution analytically
;;; &mdash; when we run our sampler, we expect to find
;;; 
;;; $$\begin{align}
;;; \mu|y\_{1:2} &\sim \mathrm{Normal}(7.25, 0.9129).
;;; \end{align}$$
;;; 
;;; We can also draw samples from the prior distribution @@\mathrm{Normal}(1,\sqrt 5)@@, to see how the posterior differs from the prior:
;; **

;; @@
(def prior-samples 
  (repeatedly 10000 #(sample* (normal 1 (sqrt 5)))))


(println "Prior on mu (blue) and posterior (green)")
(plot/compose
 (plot/histogram prior-samples
                 :normalize :probability-density :bins 40
                 :plot-range [[-10 10] [0 0.8]])
 (plot/histogram posterior-samples
                 :normalize :probability-density :bins 40
                 :color :green))
;; @@
;; ->
;;; Prior on mu (blue) and posterior (green)
;;; 
;; <-
;; =>
;;; {"type":"vega","content":{"width":400,"height":247.2187957763672,"padding":{"top":10,"left":55,"bottom":40,"right":10},"scales":[{"name":"x","type":"linear","range":"width","zero":false,"domain":[-10,10]},{"name":"y","type":"linear","range":"height","nice":true,"zero":false,"domain":[0,0.8]}],"axes":[{"type":"x","scale":"x"},{"type":"y","scale":"y"}],"data":[{"name":"1440132e-eea9-400b-8093-c77250d45b59","values":[{"x":-10.0,"y":0},{"x":-9.5,"y":0.0},{"x":-9.0,"y":0.0},{"x":-8.5,"y":0.0},{"x":-8.0,"y":0.0},{"x":-7.5,"y":0.0},{"x":-7.0,"y":2.0E-4},{"x":-6.5,"y":4.0E-4},{"x":-6.0,"y":4.0E-4},{"x":-5.5,"y":0.0016},{"x":-5.0,"y":0.0038},{"x":-4.5,"y":0.0072},{"x":-4.0,"y":0.01},{"x":-3.5,"y":0.0166},{"x":-3.0,"y":0.0302},{"x":-2.5,"y":0.0452},{"x":-2.0,"y":0.0634},{"x":-1.5,"y":0.0868},{"x":-1.0,"y":0.1104},{"x":-0.5,"y":0.1276},{"x":0.0,"y":0.1578},{"x":0.5,"y":0.1712},{"x":1.0,"y":0.1716},{"x":1.5,"y":0.1788},{"x":2.0,"y":0.1764},{"x":2.5,"y":0.147},{"x":3.0,"y":0.1312},{"x":3.5,"y":0.1104},{"x":4.0,"y":0.08},{"x":4.5,"y":0.0604},{"x":5.0,"y":0.041},{"x":5.5,"y":0.0312},{"x":6.0,"y":0.0158},{"x":6.5,"y":0.01},{"x":7.0,"y":0.0054},{"x":7.5,"y":0.0038},{"x":8.0,"y":0.0018},{"x":8.5,"y":0.001},{"x":9.0,"y":8.0E-4},{"x":9.5,"y":4.0E-4},{"x":10.0,"y":2.0E-4},{"x":10.5,"y":0.0},{"x":11.0,"y":0}]},{"name":"de78bb7a-8427-428e-9c53-6e1dba84fff5","values":[{"x":3.856171218924245,"y":0},{"x":4.011075402341908,"y":0.0032278017866816766},{"x":4.165979585759572,"y":0.0},{"x":4.320883769177235,"y":0.0032278017866816766},{"x":4.4757879525948985,"y":0.0109745260747177},{"x":4.630692136012562,"y":0.009037845002708695},{"x":4.785596319430225,"y":0.0},{"x":4.940500502847889,"y":0.0109745260747177},{"x":5.095404686265552,"y":0.023885733221444407},{"x":5.250308869683216,"y":0.022594612506771736},{"x":5.405213053100879,"y":0.055518190730924837},{"x":5.5601172365185425,"y":0.04841702680022515},{"x":5.715021419936206,"y":0.08521396716839627},{"x":5.869925603353869,"y":0.10006185538713198},{"x":6.024829786771533,"y":0.16268121004875652},{"x":6.179733970189196,"y":0.18140246041151023},{"x":6.33463815360686,"y":0.18527582255552824},{"x":6.489542337024523,"y":0.2633886257932248},{"x":6.6444465204421865,"y":0.3356913858148944},{"x":6.79935070385985,"y":0.4073485854792276},{"x":6.954254887277513,"y":0.40799414583656396},{"x":7.109159070695177,"y":0.4712590608555248},{"x":7.26406325411284,"y":0.44285440513272606},{"x":7.418967437530504,"y":0.48287914728757886},{"x":7.573871620948167,"y":0.40993082690857297},{"x":7.728775804365831,"y":0.39056401618848285},{"x":7.883679987783494,"y":0.4125130683379183},{"x":8.038584171201157,"y":0.3427925497455941},{"x":8.19348835461882,"y":0.25435078079051615},{"x":8.348392538036483,"y":0.21432603863566332},{"x":8.503296721454147,"y":0.24402181507313475},{"x":8.65820090487181,"y":0.15299780468871146},{"x":8.813105088289474,"y":0.11684642467787669},{"x":8.968009271707137,"y":0.04518922501354347},{"x":9.1229134551248,"y":0.05874599251760652},{"x":9.277817638542464,"y":0.034214698938825776},{"x":9.432721821960127,"y":0.029695776437471427},{"x":9.58762600537779,"y":0.022594612506771736},{"x":9.742530188795454,"y":0.0025822414293453413},{"x":9.897434372213118,"y":0.007101163930699689},{"x":10.052338555630781,"y":0.0032278017866816766},{"x":10.207242739048445,"y":0}]}],"marks":[{"type":"line","from":{"data":"1440132e-eea9-400b-8093-c77250d45b59"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"y":{"scale":"y","field":"data.y"},"interpolate":{"value":"step-before"},"fill":{"value":"steelblue"},"fillOpacity":{"value":0.4},"stroke":{"value":"steelblue"},"strokeWidth":{"value":2},"strokeOpacity":{"value":1}}}},{"type":"line","from":{"data":"de78bb7a-8427-428e-9c53-6e1dba84fff5"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"y":{"scale":"y","field":"data.y"},"interpolate":{"value":"step-before"},"fill":{"value":"green"},"fillOpacity":{"value":0.4},"stroke":{"value":"green"},"strokeWidth":{"value":2},"strokeOpacity":{"value":1}}}}]},"value":"#gorilla_repl.vega.VegaView{:content {:width 400, :height 247.2188, :padding {:top 10, :left 55, :bottom 40, :right 10}, :scales [{:name \"x\", :type \"linear\", :range \"width\", :zero false, :domain [-10 10]} {:name \"y\", :type \"linear\", :range \"height\", :nice true, :zero false, :domain [0 0.8]}], :axes [{:type \"x\", :scale \"x\"} {:type \"y\", :scale \"y\"}], :data ({:name \"1440132e-eea9-400b-8093-c77250d45b59\", :values ({:x -10.0, :y 0} {:x -9.5, :y 0.0} {:x -9.0, :y 0.0} {:x -8.5, :y 0.0} {:x -8.0, :y 0.0} {:x -7.5, :y 0.0} {:x -7.0, :y 2.0E-4} {:x -6.5, :y 4.0E-4} {:x -6.0, :y 4.0E-4} {:x -5.5, :y 0.0016} {:x -5.0, :y 0.0038} {:x -4.5, :y 0.0072} {:x -4.0, :y 0.01} {:x -3.5, :y 0.0166} {:x -3.0, :y 0.0302} {:x -2.5, :y 0.0452} {:x -2.0, :y 0.0634} {:x -1.5, :y 0.0868} {:x -1.0, :y 0.1104} {:x -0.5, :y 0.1276} {:x 0.0, :y 0.1578} {:x 0.5, :y 0.1712} {:x 1.0, :y 0.1716} {:x 1.5, :y 0.1788} {:x 2.0, :y 0.1764} {:x 2.5, :y 0.147} {:x 3.0, :y 0.1312} {:x 3.5, :y 0.1104} {:x 4.0, :y 0.08} {:x 4.5, :y 0.0604} {:x 5.0, :y 0.041} {:x 5.5, :y 0.0312} {:x 6.0, :y 0.0158} {:x 6.5, :y 0.01} {:x 7.0, :y 0.0054} {:x 7.5, :y 0.0038} {:x 8.0, :y 0.0018} {:x 8.5, :y 0.001} {:x 9.0, :y 8.0E-4} {:x 9.5, :y 4.0E-4} {:x 10.0, :y 2.0E-4} {:x 10.5, :y 0.0} {:x 11.0, :y 0})} {:name \"de78bb7a-8427-428e-9c53-6e1dba84fff5\", :values ({:x 3.856171218924245, :y 0} {:x 4.011075402341908, :y 0.0032278017866816766} {:x 4.165979585759572, :y 0.0} {:x 4.320883769177235, :y 0.0032278017866816766} {:x 4.4757879525948985, :y 0.0109745260747177} {:x 4.630692136012562, :y 0.009037845002708695} {:x 4.785596319430225, :y 0.0} {:x 4.940500502847889, :y 0.0109745260747177} {:x 5.095404686265552, :y 0.023885733221444407} {:x 5.250308869683216, :y 0.022594612506771736} {:x 5.405213053100879, :y 0.055518190730924837} {:x 5.5601172365185425, :y 0.04841702680022515} {:x 5.715021419936206, :y 0.08521396716839627} {:x 5.869925603353869, :y 0.10006185538713198} {:x 6.024829786771533, :y 0.16268121004875652} {:x 6.179733970189196, :y 0.18140246041151023} {:x 6.33463815360686, :y 0.18527582255552824} {:x 6.489542337024523, :y 0.2633886257932248} {:x 6.6444465204421865, :y 0.3356913858148944} {:x 6.79935070385985, :y 0.4073485854792276} {:x 6.954254887277513, :y 0.40799414583656396} {:x 7.109159070695177, :y 0.4712590608555248} {:x 7.26406325411284, :y 0.44285440513272606} {:x 7.418967437530504, :y 0.48287914728757886} {:x 7.573871620948167, :y 0.40993082690857297} {:x 7.728775804365831, :y 0.39056401618848285} {:x 7.883679987783494, :y 0.4125130683379183} {:x 8.038584171201157, :y 0.3427925497455941} {:x 8.19348835461882, :y 0.25435078079051615} {:x 8.348392538036483, :y 0.21432603863566332} {:x 8.503296721454147, :y 0.24402181507313475} {:x 8.65820090487181, :y 0.15299780468871146} {:x 8.813105088289474, :y 0.11684642467787669} {:x 8.968009271707137, :y 0.04518922501354347} {:x 9.1229134551248, :y 0.05874599251760652} {:x 9.277817638542464, :y 0.034214698938825776} {:x 9.432721821960127, :y 0.029695776437471427} {:x 9.58762600537779, :y 0.022594612506771736} {:x 9.742530188795454, :y 0.0025822414293453413} {:x 9.897434372213118, :y 0.007101163930699689} {:x 10.052338555630781, :y 0.0032278017866816766} {:x 10.207242739048445, :y 0})}), :marks ({:type \"line\", :from {:data \"1440132e-eea9-400b-8093-c77250d45b59\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :y {:scale \"y\", :field \"data.y\"}, :interpolate {:value \"step-before\"}, :fill {:value \"steelblue\"}, :fillOpacity {:value 0.4}, :stroke {:value \"steelblue\"}, :strokeWidth {:value 2}, :strokeOpacity {:value 1}}}} {:type \"line\", :from {:data \"de78bb7a-8427-428e-9c53-6e1dba84fff5\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :y {:scale \"y\", :field \"data.y\"}, :interpolate {:value \"step-before\"}, :fill {:value :green}, :fillOpacity {:value 0.4}, :stroke {:value :green}, :strokeWidth {:value 2}, :strokeOpacity {:value 1}}}})}}"}
;; <=

;; **
;;; # The seven scientists
;;; 
;;; Here's an interesting variation on estimating the mean of a Gaussian. This example is from [MacKay 2003, exercise 22.15] and [Lee & Wagenmaker 2013, section 4.2].
;;; 
;;; Suppose seven scientists all go and perform the same experiment, each collecting a measurement @@y\_i@@ for @@i = 1,\dots,7@@. 
;;; 
;;; These scientists are varyingly good at their job, and while we can assume each scientist would estimate @@y@@ correctly _on average_, some of them may have much more error in their measurements than others.
;;; 
;;; They come back with the following seven observations:
;; **

;; @@
(def measurements [-27.020 3.570 8.191 9.898 9.603 9.945 10.056])

(plot/bar-chart (range 1 8) measurements)
;; @@
;; =>
;;; {"type":"vega","content":{"width":400,"height":247.2187957763672,"padding":{"top":10,"left":55,"bottom":40,"right":10},"data":[{"name":"4b51d8ed-67a5-4b62-aa2d-e63eb9496921","values":[{"x":1,"y":-27.02},{"x":2,"y":3.57},{"x":3,"y":8.191},{"x":4,"y":9.898},{"x":5,"y":9.603},{"x":6,"y":9.945},{"x":7,"y":10.056}]}],"marks":[{"type":"rect","from":{"data":"4b51d8ed-67a5-4b62-aa2d-e63eb9496921"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"width":{"scale":"x","band":true,"offset":-1},"y":{"scale":"y","field":"data.y"},"y2":{"scale":"y","value":0}},"update":{"fill":{"value":"steelblue"},"opacity":{"value":1}},"hover":{"fill":{"value":"#FF29D2"}}}}],"scales":[{"name":"x","type":"ordinal","range":"width","domain":{"data":"4b51d8ed-67a5-4b62-aa2d-e63eb9496921","field":"data.x"}},{"name":"y","range":"height","nice":true,"domain":{"data":"4b51d8ed-67a5-4b62-aa2d-e63eb9496921","field":"data.y"}}],"axes":[{"type":"x","scale":"x"},{"type":"y","scale":"y"}]},"value":"#gorilla_repl.vega.VegaView{:content {:width 400, :height 247.2188, :padding {:top 10, :left 55, :bottom 40, :right 10}, :data [{:name \"4b51d8ed-67a5-4b62-aa2d-e63eb9496921\", :values ({:x 1, :y -27.02} {:x 2, :y 3.57} {:x 3, :y 8.191} {:x 4, :y 9.898} {:x 5, :y 9.603} {:x 6, :y 9.945} {:x 7, :y 10.056})}], :marks [{:type \"rect\", :from {:data \"4b51d8ed-67a5-4b62-aa2d-e63eb9496921\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :width {:scale \"x\", :band true, :offset -1}, :y {:scale \"y\", :field \"data.y\"}, :y2 {:scale \"y\", :value 0}}, :update {:fill {:value \"steelblue\"}, :opacity {:value 1}}, :hover {:fill {:value \"#FF29D2\"}}}}], :scales [{:name \"x\", :type \"ordinal\", :range \"width\", :domain {:data \"4b51d8ed-67a5-4b62-aa2d-e63eb9496921\", :field \"data.x\"}} {:name \"y\", :range \"height\", :nice true, :domain {:data \"4b51d8ed-67a5-4b62-aa2d-e63eb9496921\", :field \"data.y\"}}], :axes [{:type \"x\", :scale \"x\"} {:type \"y\", :scale \"y\"}]}}"}
;; <=

;; **
;;; Clearly scientist 1 does not know what he is doing (and 2 and 3 are probably a little suspect too)!
;;; 
;;; To model this situation, we place simple priors on the mean @@\mu@@ of the measurements, and the error standard deviation @@\sigma\_i@@ for each of the @@i@@ scientists.
;;; 
;;; As a starting point, consider placing uninformative priors on these parameters; a suggestion is:
;;; $$\begin{align}
;;; \mu &\sim \mathrm{Normal}(0, 50) \\\\
;;; \sigma\_i &\sim \mathrm{Uniform}(0, 25)
;;; \end{align}$$
;;; 
;;; The uniform distribution over real numbers on the open interval @@(a, b)@@ can be constructed in Anglican with `(uniform-continuous a b)`.
;;; 
;;; We can ask two questions here:
;;; 
;;; * Given these measurements, what is the posterior distribution of @@y@@?
;;; * What distribution over noise level @@\sigma_i@@ do we infer for each of these scientists' estimates?
;;; 
;;; Write the model in Anglican such that it returns a hashmap with two entries: one for the value `:mu` and one for the vector of seven noise levels `:sigmas`.
;; **

;; @@
(defquery scientists [measurements]
  (let [;; sample the mean (same for each scientist)
        mu (sample (normal 0 50))
        sigmas (map (fn [y]
                      (let [;; sample the measurement noise 
                            ;; (specific to scientist)
                            sigma (sample (uniform-continuous 0 25))]
                        ;; observe the measured value
                        (observe (normal mu sigma) y)
                        ;; return the sampled noise
                        sigma))
                    measurements)]
    ;; return mean and measurement noise for each scientist
    {:mu mu :sigmas sigmas}))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;gaussian-estimation/scientists</span>","value":"#'gaussian-estimation/scientists"}
;; <=

;; **
;;; Given the measurements, we sample from the conditional distribution and plot a histogram of the results.
;; **

;; @@
(def scientist-samples 
  (take 10000
        (drop 10000
              (map :result
                   (doquery :rmh 
                            scientists [measurements])))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;gaussian-estimation/scientist-samples</span>","value":"#'gaussian-estimation/scientist-samples"}
;; <=

;; @@
(println "Expected value of measured quantity:" 
         (mean (map :mu scientist-samples)))

(plot/histogram (map :mu scientist-samples)
                :normalize :probability
                :bins 50)
;; @@
;; ->
;;; Expected value of measured quantity: 9.006662279997045
;;; 
;; <-
;; =>
;;; {"type":"vega","content":{"width":400,"height":247.2187957763672,"padding":{"top":10,"left":55,"bottom":40,"right":10},"data":[{"name":"9b5b7e59-60d4-4202-bbf3-fa365865d78a","values":[{"x":-4.009064776501177,"y":0},{"x":-3.634475801326413,"y":0.006},{"x":-3.259886826151649,"y":0.0028},{"x":-2.8852978509768854,"y":0.0},{"x":-2.5107088758021217,"y":0.0},{"x":-2.136119900627358,"y":0.001},{"x":-1.7615309254525944,"y":1.0E-4},{"x":-1.3869419502778308,"y":0.0},{"x":-1.0123529751030673,"y":0.0},{"x":-0.6377639999283038,"y":0.0},{"x":-0.2631750247535402,"y":1.0E-4},{"x":0.11141395042122337,"y":0.0},{"x":0.48600292559598696,"y":0.0022},{"x":0.8605919007707505,"y":0.0083},{"x":1.235180875945514,"y":0.0},{"x":1.6097698511202776,"y":0.0074},{"x":1.9843588262950411,"y":0.0106},{"x":2.358947801469805,"y":0.0062},{"x":2.7335367766445686,"y":0.0},{"x":3.1081257518193324,"y":0.0},{"x":3.482714726994096,"y":0.0},{"x":3.85730370216886,"y":0.0014},{"x":4.231892677343623,"y":0.0073},{"x":4.6064816525183865,"y":0.0},{"x":4.98107062769315,"y":0.0},{"x":5.355659602867913,"y":0.0021},{"x":5.730248578042676,"y":0.0},{"x":6.10483755321744,"y":0.0},{"x":6.479426528392203,"y":0.0075},{"x":6.854015503566966,"y":0.003},{"x":7.22860447874173,"y":0.0331},{"x":7.603193453916493,"y":0.0274},{"x":7.977782429091256,"y":0.0325},{"x":8.35237140426602,"y":0.0478},{"x":8.726960379440783,"y":0.0443},{"x":9.101549354615546,"y":0.0532},{"x":9.47613832979031,"y":0.0752},{"x":9.850727304965073,"y":0.2093},{"x":10.225316280139836,"y":0.3018},{"x":10.5999052553146,"y":0.0552},{"x":10.974494230489363,"y":0.0186},{"x":11.349083205664126,"y":0.0149},{"x":11.72367218083889,"y":0.0086},{"x":12.098261156013653,"y":0.002},{"x":12.472850131188416,"y":0.0},{"x":12.84743910636318,"y":0.0},{"x":13.222028081537943,"y":0.0},{"x":13.596617056712706,"y":0.0},{"x":13.97120603188747,"y":0.0044},{"x":14.345795007062232,"y":0.0032},{"x":14.720383982236996,"y":0.0012},{"x":15.094972957411759,"y":0.0013},{"x":15.469561932586522,"y":0}]}],"marks":[{"type":"line","from":{"data":"9b5b7e59-60d4-4202-bbf3-fa365865d78a"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"y":{"scale":"y","field":"data.y"},"interpolate":{"value":"step-before"},"fill":{"value":"steelblue"},"fillOpacity":{"value":0.4},"stroke":{"value":"steelblue"},"strokeWidth":{"value":2},"strokeOpacity":{"value":1}}}}],"scales":[{"name":"x","type":"linear","range":"width","zero":false,"domain":{"data":"9b5b7e59-60d4-4202-bbf3-fa365865d78a","field":"data.x"}},{"name":"y","type":"linear","range":"height","nice":true,"zero":false,"domain":{"data":"9b5b7e59-60d4-4202-bbf3-fa365865d78a","field":"data.y"}}],"axes":[{"type":"x","scale":"x"},{"type":"y","scale":"y"}]},"value":"#gorilla_repl.vega.VegaView{:content {:width 400, :height 247.2188, :padding {:top 10, :left 55, :bottom 40, :right 10}, :data [{:name \"9b5b7e59-60d4-4202-bbf3-fa365865d78a\", :values ({:x -4.009064776501177, :y 0} {:x -3.634475801326413, :y 0.006} {:x -3.259886826151649, :y 0.0028} {:x -2.8852978509768854, :y 0.0} {:x -2.5107088758021217, :y 0.0} {:x -2.136119900627358, :y 0.001} {:x -1.7615309254525944, :y 1.0E-4} {:x -1.3869419502778308, :y 0.0} {:x -1.0123529751030673, :y 0.0} {:x -0.6377639999283038, :y 0.0} {:x -0.2631750247535402, :y 1.0E-4} {:x 0.11141395042122337, :y 0.0} {:x 0.48600292559598696, :y 0.0022} {:x 0.8605919007707505, :y 0.0083} {:x 1.235180875945514, :y 0.0} {:x 1.6097698511202776, :y 0.0074} {:x 1.9843588262950411, :y 0.0106} {:x 2.358947801469805, :y 0.0062} {:x 2.7335367766445686, :y 0.0} {:x 3.1081257518193324, :y 0.0} {:x 3.482714726994096, :y 0.0} {:x 3.85730370216886, :y 0.0014} {:x 4.231892677343623, :y 0.0073} {:x 4.6064816525183865, :y 0.0} {:x 4.98107062769315, :y 0.0} {:x 5.355659602867913, :y 0.0021} {:x 5.730248578042676, :y 0.0} {:x 6.10483755321744, :y 0.0} {:x 6.479426528392203, :y 0.0075} {:x 6.854015503566966, :y 0.003} {:x 7.22860447874173, :y 0.0331} {:x 7.603193453916493, :y 0.0274} {:x 7.977782429091256, :y 0.0325} {:x 8.35237140426602, :y 0.0478} {:x 8.726960379440783, :y 0.0443} {:x 9.101549354615546, :y 0.0532} {:x 9.47613832979031, :y 0.0752} {:x 9.850727304965073, :y 0.2093} {:x 10.225316280139836, :y 0.3018} {:x 10.5999052553146, :y 0.0552} {:x 10.974494230489363, :y 0.0186} {:x 11.349083205664126, :y 0.0149} {:x 11.72367218083889, :y 0.0086} {:x 12.098261156013653, :y 0.002} {:x 12.472850131188416, :y 0.0} {:x 12.84743910636318, :y 0.0} {:x 13.222028081537943, :y 0.0} {:x 13.596617056712706, :y 0.0} {:x 13.97120603188747, :y 0.0044} {:x 14.345795007062232, :y 0.0032} {:x 14.720383982236996, :y 0.0012} {:x 15.094972957411759, :y 0.0013} {:x 15.469561932586522, :y 0})}], :marks [{:type \"line\", :from {:data \"9b5b7e59-60d4-4202-bbf3-fa365865d78a\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :y {:scale \"y\", :field \"data.y\"}, :interpolate {:value \"step-before\"}, :fill {:value \"steelblue\"}, :fillOpacity {:value 0.4}, :stroke {:value \"steelblue\"}, :strokeWidth {:value 2}, :strokeOpacity {:value 1}}}}], :scales [{:name \"x\", :type \"linear\", :range \"width\", :zero false, :domain {:data \"9b5b7e59-60d4-4202-bbf3-fa365865d78a\", :field \"data.x\"}} {:name \"y\", :type \"linear\", :range \"height\", :nice true, :zero false, :domain {:data \"9b5b7e59-60d4-4202-bbf3-fa365865d78a\", :field \"data.y\"}}], :axes [{:type \"x\", :scale \"x\"} {:type \"y\", :scale \"y\"}]}}"}
;; <=

;; @@
(def noise-estimate 
  (mean (map :sigmas scientist-samples)))

(plot/bar-chart (range 1 8) noise-estimate)
;; @@
;; =>
;;; {"type":"vega","content":{"width":400,"height":247.2187957763672,"padding":{"top":10,"left":55,"bottom":40,"right":10},"data":[{"name":"0740d6df-3b8e-4e96-924c-dda94ef60c32","values":[{"x":1,"y":20.503180520082946},{"x":2,"y":11.67329438839822},{"x":3,"y":8.350191490878753},{"x":4,"y":7.234342971063601},{"x":5,"y":6.746406658461678},{"x":6,"y":6.351884468116099},{"x":7,"y":6.240478605166342}]}],"marks":[{"type":"rect","from":{"data":"0740d6df-3b8e-4e96-924c-dda94ef60c32"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"width":{"scale":"x","band":true,"offset":-1},"y":{"scale":"y","field":"data.y"},"y2":{"scale":"y","value":0}},"update":{"fill":{"value":"steelblue"},"opacity":{"value":1}},"hover":{"fill":{"value":"#FF29D2"}}}}],"scales":[{"name":"x","type":"ordinal","range":"width","domain":{"data":"0740d6df-3b8e-4e96-924c-dda94ef60c32","field":"data.x"}},{"name":"y","range":"height","nice":true,"domain":{"data":"0740d6df-3b8e-4e96-924c-dda94ef60c32","field":"data.y"}}],"axes":[{"type":"x","scale":"x"},{"type":"y","scale":"y"}]},"value":"#gorilla_repl.vega.VegaView{:content {:width 400, :height 247.2188, :padding {:top 10, :left 55, :bottom 40, :right 10}, :data [{:name \"0740d6df-3b8e-4e96-924c-dda94ef60c32\", :values ({:x 1, :y 20.503180520082946} {:x 2, :y 11.67329438839822} {:x 3, :y 8.350191490878753} {:x 4, :y 7.234342971063601} {:x 5, :y 6.746406658461678} {:x 6, :y 6.351884468116099} {:x 7, :y 6.240478605166342})}], :marks [{:type \"rect\", :from {:data \"0740d6df-3b8e-4e96-924c-dda94ef60c32\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :width {:scale \"x\", :band true, :offset -1}, :y {:scale \"y\", :field \"data.y\"}, :y2 {:scale \"y\", :value 0}}, :update {:fill {:value \"steelblue\"}, :opacity {:value 1}}, :hover {:fill {:value \"#FF29D2\"}}}}], :scales [{:name \"x\", :type \"ordinal\", :range \"width\", :domain {:data \"0740d6df-3b8e-4e96-924c-dda94ef60c32\", :field \"data.x\"}} {:name \"y\", :range \"height\", :nice true, :domain {:data \"0740d6df-3b8e-4e96-924c-dda94ef60c32\", :field \"data.y\"}}], :axes [{:type \"x\", :scale \"x\"} {:type \"y\", :scale \"y\"}]}}"}
;; <=

;; **
;;; * Are these noise levels what you would expect?
;;; * How sensitive is this to the prior on @@\mu@@ and @@\sigma\_i@@?
;; **

;; **
;;; 
;; **
