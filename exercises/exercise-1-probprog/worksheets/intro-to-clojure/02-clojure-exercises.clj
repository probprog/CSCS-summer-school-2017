;; gorilla-repl.fileformat = 1

;; **
;;; # Clojure Exercises
;; **

;; @@
(ns clojure-exercises
   (:require [clojure.repl :as repl])
   (:use [anglican.runtime :exclude [sum]]))

(def ...complete-this... nil)

(defmacro dbg 
  "Prints an expression and
  its value for debugging."
  [expr]
  `(let [value# ~expr] 
     (println "[dbg]" '~expr value#) 
     value#)) 
;; @@

;; **
;;; ## Exercise 1: Summing values
;;; 
;;; If you've gone through the `getting-started.clj` worksheet, you will have seen the `loop` construct. We will use `loop` to define a `sum` function.
;;; 
;;; Complete the function below by replacing `...complete-this...` with the correct expressions
;; **

;; @@
(defn sum 
  "returns the sum of values in a collection"
  [values]
  (loop [result 0.0
         values values]
    (if (seq values)
      (recur ...complete-this...
             ...complete-this...)
      result)))
;; @@

;; **
;;; You can test your function using the command
;; **

;; @@
(sum [1 2 3])
; => 6.0
;; @@

;; **
;;; Note that the sum function returns a double, whereas the inputs are longs. Can you explain why this happens?
;;; 
;;; Rewrite the sum function so it preserves the type of the input
;; **

;; @@
(defn sum 
  "returns the sum of values in a collection"
  [values]
  (loop [result nil
         values values]
    (if (seq values)
      (if result
        (recur ...complete-this...
               ...complete-this...)
        (recur ...complete-this...
               ...complete-this...))
      result)))
;; @@

;; **
;;; Now test that the sum function preserves input types
;; **

;; @@
(sum [1.0 2.0 3.0])
; => 6.0


(sum [1 2 3])
; => 6
;; @@

;; **
;;; Rewrite the `sum` function using the `reduce` command
;; **

;; @@
(defn sum [values]
  ...complete-this...)
;; @@

;; @@
(sum [1.0 2.0 3.0])
; => 6.0


(sum [1 2 3])
; => 6
;; @@

;; **
;;; Now let's write a cumulative sum function that maps a collection of numbers `[1 2 3 4]` onto the partial sums `[1 3 6 10]`
;; **

;; @@
(defn cumsum 
  "returns a vector of partial sums"
  [values]
  (loop [results nil
         values values]
    (if (seq values)
      (if results
        (recur ...complete-this...
               ...complete-this...)
        (recur ...complete-this...
               ...complete-this...))
      results)))
;; @@

;; **
;;; Test your function
;; **

;; @@
(cumsum (list 1 2 3 4))
; => [1 3 6 10]
;; @@

;; **
;;; Bonus: can you write `cumsum` as a function that returns a lazy sequence?
;; **

;; @@
(defn cumsum 
  "returns a vector of partial sums"
  ([values]
   (if (seq values)
      ...complete-this...
     (lazy-seq)))
  ([init values]
   (lazy-seq
	  ...complete-this...)))
;; @@

;; **
;;; Again, test your function
;; **

;; @@
(cumsum [1 2 3 4])
; => (1 3 6 10)
;; @@

;; **
;;; ## Exercise 2: Higher-order functions
;; **

;; **
;;; In the `getting-started` worksheet, we used the `map` function. This function is an example of a higher-order function, which is to say that it is a function that accepts a function as an argument. In this exercise we will look at some of clojure's higher-order functions, and write implementations of our own.
;; **

;; **
;;; ### Exercise 2a: Map
;;; 
;;; Let's start with `mapv`, which is a variant of `map` that returns a vector. 
;; **

;; @@
(defn my-mapv 
  [f values]
  (loop [results []
         values values]
    ...complete-this...))
;; @@

;; **
;;; Test your `mapv` function
;; **

;; @@
(my-mapv #(* % %) 
         (range 5))
; => [0 1 4 9 16]

(my-mapv #(* % %) nil)
; => []
;; @@

;; **
;;; Now write the `map` function (which should return a lazy sequence)
;; **

;; @@
(defn my-map 
  [f values]	
  (lazy-seq
    (when (seq values)
      ...complete-this...)))
;; @@

;; @@
(my-map #(* % %) (range 5))
; => (0 1 4 9 16)

(my-map #(* % %) nil)
; => ()
;; @@

;; **
;;; ### Exercise 2b: Comp
;;; 
;;; We will now define a function `comp`, which takes two functions `f` and `g` as an argument and returns a function `h` such that `(h x) -> (f (g x))`. 
;;; 
;;; Let's start by considering the case where `f` and `g` accept a single argument. Complete the following code
;; **

;; @@
(defn my-comp [f g]
  (fn [x]
    ...complete-this...))
;; @@

;; **
;;; Test this code by composing `sqrt` and `sqr`:
;; **

;; @@
(let [f sqrt
      g (fn [x] 
         (* x x))
      h (my-comp f g)]
  (h -10))
;; @@

;; **
;;; Now let's generalize this function to accept a variable number of arguments. The following code defines a function with an `args` that accepts a variable number of arguments
;; **

;; @@
(let [f (fn [& args]
          (prn args))]
  (f 1)
  (f 2 3 4))
;; @@

;; **
;;; In order to pass a variable length set of arguments to a function, we will need the `apply` function, which can be called `(apply f args)` to call a function with a (variable length) sequence of arguments `args`.  
;; **

;; @@
(let [args [1 2 3]]
  ;; this is equivalent to (+ 1 2 3)
  (apply + args))
;; @@

;; **
;;; Use the `apply` function to define a `comp` function that is agnostic of the number of input arguments
;; **

;; @@
(defn my-comp [f g]
  ...complete-this...)
;; @@

;; **
;;; Test your function
;; **

;; @@
(let [f (comp abs *)]
  (f -1 2 -3 4 -5))
; => 120
;; @@

;; **
;;; We can look up how the Clojure function `comp` is defined using the `source` command from the `clojure.repl` namespace
;; **

;; @@
(repl/source comp)
;; @@

;; **
;;; We see that this function has 4 signatures:
;;; 
;;; 1. `(comp)` with no arguments returns the identity function `(fn [x] x)`
;;; 2. `(comp f)` with a single argument returns the function `f`
;;; 3. `(comp f g)` returns the composition of `f` and `g`
;;; 4. `(comp f g & fs)` returns the composition of a variable number of functions
;;; 
;;; The implementation for `(comp f g)` itself specifies a function with 5 signatures. 
;;; 
;;; - Why do you think the Clojure developers have gone through the trouble of defining these call syntaxes?
;;; - Can you infer what the syntax `(apply g x y z args)` does?
;; **

;; **
;;; ### Exercise 2c Reduce
;;; 
;;; As a final exercise, let's implement the `reduce` function. This function has two signatures:
;;; 
;;; 1. `(reduce f init values)`: repeatedly call `(f result value)` for each `value` in `values` where `result` is the result of the previous function call, and is initialized to `init`.
;;; 2. `(reduce f values)`: perform the above operation, initializing `init` to `(first values)` and replacing `values` with `(rest values)`. 
;;; 
;;; Complete the following code (*hint*: look at the `loop` and `recur` patterns from Exercise 1)
;; **

;; @@
(defn my-reduce 
  ([f init values]
   ...complete-this...)
  ([f values]
   ...complete-this...))
;; @@

;; **
;;; Test your code
;; **

;; @@
(my-reduce + [1 2 3 4])

; => 10

(my-reduce (fn [sums v]
             (conj sums
                   (if (seq sums)
                     (+ (peek sums) v)
                     v)))
           []
           [1 2 3 4])
; => [1 3 6 10]

(my-reduce + nil)
; => 0
;; @@

;; **
;;; *Bonus*: Clojure implements a function called `reductions`, which performs a `reduce` operation and returns a lazy sequence of intermediate results. In other words `(reductions + [1 2 3 4])` returns a lazy sequence `(1 3 6 10)`. Write this function.
;; **

;; @@
(defn my-reductions
  ([f init values]
   ...complete-this...)
  ([f values]
   ...complete-this...))
;; @@

;; **
;;; and test your code	
;; **

;; @@
(my-reductions + [1 2 3 4])
; => (1 3 6 10)
;; @@

;; @@
;; once you are certain that you are returning a lazy sequence

(defn inf-range 
  "returns an infinite range as a lazy sequence"
  ;; this defines the default call syntax
  ([start step]
  	(lazy-seq 
      (cons start 
            (inf-range (+ start step) 
                       step))))
  ;; this defines a call syntax with 
  ;; the start argument omitted
  ([step] 
   (inf-range 0 step))
  ;; this defines a call syntax with
  ;; no arguments
  ([]
   (inf-range 0 1)))

(take 10 (my-reductions + (inf-range)))
; => (0 1 3 6 10 15 21 28 36 45)
;; @@

;; **
;;; ## Exercise 3: Recursion
;; **

;; **
;;; For our last exercise, we will consider recursive functions. Suppose we have the following problem: somebody has written a function that returns complicated nested data structures. Say we have something of the form
;;; 
;; **

;; @@
{:a (list "1" "2.0" "-3" "4.0e0" "5N")
 :b [["1" 2] ["3" 4]]
 :c #{1 "2" 3}
 :d {"1" "red" "2" "green" "3" "blue"} 
 :e nil}
;; @@

;; **
;;; Can we now write a function that replaces all strings corresponding to numbers with their numeric equivalents, to produce
;; **

;; @@
{:a (list 1 2.0 -3 4.0 5N)
 :b [[1 2] [3 4]]
 :c #{1 3 2}
 :d {1 "red", 2 "green", 3 "blue"}
 :e nil}
;; @@

;; **
;;; We will break this problem down into subproblems. In order to be able to convert data structures we need to be able to handle
;;; 
;;; 1. lists
;;; 2. vectors
;;; 3. lazy sequences
;;; 4. hash maps
;;; 5. sets (and any other collections)
;; **

;; **
;;; Let's start by writing a function that converts a string to a number. You can assume the input is non-malicious and use the function `read-string` to parse strings into Clojure data. *Hint:* use the `number?` function to test if an object is a number.
;; **

;; @@
(defn numerify
  "try to parse a string as a number, do nothing when 
  the string is does not parse to a number or for any
  other type of object"
  [v]
  (if (string? v)
    ...complete-this...
    v))
;; @@

;; **
;;; Tests
;; **

;; @@
(numerify "hello")
; => "hello"

(numerify "-3.14159e0")
; => -3.14159

(numerify ["1" "2" "3"])
; => ["1" "2" "3"]
;; @@

;; **
;;; Now let's write a function that can handle sequences. For each element in a list or vector, call `numerify` recursively, and make sure the result always has the same type as the input.
;; **

;; @@
(defn numerify
  "try to parse a string as a number, do nothing when 
  the string is does not parse to a number or for any
  other type of object"
  [v]
  (cond 
    (string? v)
    ...complete-this...
    (seq? v)
    ...complete-this...		;; Hint: Use map!
	:else v))
;; @@

;; **
;;; Tests
;; **

;; @@
(numerify (list "1" 2 "3"))
; => (1 2 3)

(numerify (list (list 1 "2") 
                (list "3" 4)))
; => ((1 2) (3 4))

(numerify ["1" 2 "3"])
; => ["1" 2 "3"]
;; @@

;; **
;;; There is a small problem with the code above: To see this, look at the class of the output of a numerified list
;; **

;; @@
(class (list "1" 2 "3"))
(class (numerify (list "1" 2 "3")))
;; @@

;; **
;;; The reason for this is that Clojure differentiates between lists and sequences. A list is a sequence, but a sequence is not necessarily a list. 
;;; 
;;; You can use `list?` to test whether a sequence is a list. Let's handle this case separately. While we're at it, let's also make sure we handle vectors
;; **

;; @@
(defn numerify
  "try to parse a string as a number, do nothing when 
  the string is does not parse to a number or for any
  other type of object"
  [v]
  (cond 
    (string? v)
    ...complete-this...
    (list? v)
    ...complete-this...
    (vector? v)
    ...complete-this...
    (seq? v)
    ...complete-this...
	:else v))
;; @@

;; **
;;; Tests
;; **

;; @@
(numerify (list "1" 2 "3"))
; => (1 2 3)

(list? (numerify (list "1" 2 "3")))
; => true

(numerify (lazy-seq (cons 1 (list 2 "3"))))
; => (1 2 3)

(list? (numerify (lazy-seq (cons 1 (list 2 "3")))))
; => false

(seq? (numerify (lazy-seq (cons 1 (list 2 "3")))))
; => true

(numerify [["1" 2] [3 "4"]])
; => [[1 2] [3 4]]
;; @@

;; **
;;; Now let's move on to hash maps and any other collection types like sets. 
;;; 
;;; You may remember from the `getting-started` worksheet that a hashmap, like any collection, can be iterated over as a sequence. For a hash map the elements of this are vector pairs. 
;; **

;; @@
(seq {:a 1 :b 2})
;; @@

;; **
;;; These look like vectors, but in fact have a special type. This type is a subtype of a vector
;; **

;; @@
(class (first {:a 1 :b 2}))
(vector? (first {:a 1 :b 2}))
;; @@

;; **
;;; The command `into` can be used to turn a sequence of pairs back into a hashmap
;; **

;; @@
(into {} [[:a 1] [:b 2]])
;; @@

;; **
;;; this command is functionally equivalent to
;; **

;; @@
(reduce conj {} [[:a 1] [:b 2]])
;; @@

;; **
;;; using `map?` to test whether an object is a hash map, now extend the numerify function
;; **

;; @@
(defn numerify
  "try to parse a string as a number, do nothing when 
  the string is does not parse to a number or for any
  other type of object"
  [v]
  (cond 
    (string? v)
    ...complete-this...
    (list? v)
    ...complete-this...
    (vector? v)
    ...complete-this...
    (seq? v)
    ...complete-this...
    (map? v)
    ...complete-this...
	:else v))
;; @@

;; **
;;; Test your result
;; **

;; @@
(numerify {1 "2" "3" 4 :a 5})
;; => {1 2, 3 4, :a 5}
;; @@

;; **
;;; Now that we have hash maps working, the only other core data type that we need to think about are sets. 
;;; 
;;; It turns out that the `into` trick does not only work for hash maps, it also works for sets, and collections in general 
;; **

;; @@
(into #{} [1 2 3])
;; @@

;; **
;;; Lookup the function `empty`, and use it to generalize your solution for hash maps to other collection types
;; **

;; @@
(defn numerify
  "try to parse a string as a number, do nothing when 
  the string is does not parse to a number or for any
  other type of object"
  [v]
  (cond 
    (string? v)
    ...complete-this...
    (list? v)
    ...complete-this...
    (vector? v)
    ...complete-this...
    (seq? v)
    ...complete-this...
    (coll? v)
    ...complete-this...
	:else v))
;; @@

;; **
;;; You have now written a function that can walk an arbitrary nested Clojure data types in just over 10 lines of code. That's pretty ninja, especially if this is your first day as a Clojure programmer. Test your result! 
;; **

;; @@
(numerify 
  {:a (list "1" "2.0" "-3" "4.0e0" "5N")
   :b [["1" 2] ["3" 4]]
   :c #{1 "2" 3}
   :d {"1" "red" "2" "green" "3" "blue"} 
   :e nil})
;; => {:a (1 2.0 -3 4.0 5N), 
;;     :b [[1 2] [3 4]], 
;;     :c #{1 3 2}, 
;;     :d {1 "red", 2 "green", 3 "blue"}, 
;;     :e nil} 
;; @@

;; **
;;; *Bonus*: Since `into` can be used on any collection, it also works for vectors 
;; **

;; @@
(let [v [-1 2 -3]]
  (into (empty v) 
        (map abs v)))
;; @@

;; **
;;; You should therefore be able to numerify vectors with the `(coll? v)` clause. Try removing the `(vector? v)` clause from your solution. Can you explain why the resulting code no longer works? *Hint*: try calling `empty` on a `MapEntry` value.
;;; 
;;; Why should you not use `into` for lists and sequences in this function?
;; **
