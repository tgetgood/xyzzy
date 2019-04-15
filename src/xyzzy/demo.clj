(ns xyzzy.demo
  (:require [xyzzy.codebase :refer :all]
            [xyzzy.res.code-gen :as gen]))

(use-branch :master)

(defsn :foo/f
  (fn [x] (* 2 x)))

(fork-branch :master :alice)

(with-versions {f {:branch :master :name :foo/f}}
  (defsn :foo/g
    (fn [x] (f (inc x)))))

(inspect :master :foo/f)

((gen/invoke-by-id "53d13") 4)



(fork-branch :master :bob)

(defsn :foo/f
  (fn [x y] (+ (* 2 x) y)))

(inspect-branch :master)
