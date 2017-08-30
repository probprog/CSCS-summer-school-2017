(defproject exercises "0.1.0-SNAPSHOT"
  :description "CSCS Summer School 2017: Inference Compilation Exercises"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-gorilla "0.4.0"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [anglican "1.1.0-SNAPSHOT"]
                 [anglican-infcomp "0.2.3-SNAPSHOT"]]
  :java-source-paths ["src/java"]
  :main exercises.core)
