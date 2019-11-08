(ns xyzzy.collision
  (:require [clojure.core.reducers :as r]))

(defn collision [num hash-size]
  (let [m (bigdec (Math/pow 2 hash-size))]
    (with-precision 100
      (- 1M (r/reduce * 1 (r/map #(/ (- m %) m) (range num)))))))

;; Way faster(~35X), but a tad less precise.
(defn logc [num size]
  (let [hs (Math/pow 2 size)
        diff (* size (Math/log 2))
        logs (r/reduce + (r/map #(- (Math/log (- hs %)) diff) (range num)))]
    (- 1 (Math/exp logs))))
