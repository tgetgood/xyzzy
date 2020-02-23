(ns xyzzy.collision
  (:require [clojure.core.reducers :as r]))

(defn p-no-collisions
  "Returns the probability that we get `items` things into a hash-table of size
  2^`hash-size` without any collisions."
  [items hash-size]
  (let [N (- (Math/pow 2 (- hash-size)))]
    (inc (Math/expm1 (* (dec items) (Math/log1p N))))))

(defn ex-collision
  "Returns the expected number of collisions when putting `items` items into a
  hash table of size 2^`hash-size`."
  [items hash-size]
  (* items (- 1 (p-no-collisions items hash-size))))
