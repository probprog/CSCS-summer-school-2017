(ns examples.factorial
  (:gen-class))

(defn factorial 
  "computes n * (n-1) * ... * 1"
  [n]
  (if (= n 1)
    1
    (* n (factorial (- n 1)))))

(defn -main 
  [& args]
  (doseq [arg args]
    (let [n (Long/parseLong arg)]
      (println "the factorial of" arg 
              "is" (factorial n)))))


