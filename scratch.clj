;; This buffer is for Clojure experiments and evaluation.

;; Press C-j to evaluate the last expression.

;; You can also press C-u C-j to evaluate the expression and pretty-print its result.

(in-ns 'xyzzy.core)

(inspect-branch :master)

(new-branch :master)

(defsn :foo/f
  (fn [x] (* 2 x)))

(inspect :master :foo/f)

{:sha1 "c255a", :dependencies {}, :form (fn [x] (* 2 x))}

(fork-branch :master :alice)

(with-versions {f "c255a"}
  (defsn :foo/g
    (fn [x] (f (inc x)))))

(inspect-branch :alice)

(fork-branch :master :bob)


(inspect-branch :bob)

{"foo"
 {"f"
  {:name :foo/f,
   :ref "c255a",
   :time #inst "2019-04-16T00:18:00.100-00:00",
   :op :add}}}

(defsn :foo/f
  (fn [x y] (+ (* 2 x) y)))

{:form (fn [x y] (+ (* 2 x) y)), :links {}}

(inspect :bob :foo/f)

{:sha1 "e073c", :dependencies {}, :form (fn [x y] (+ (* 2 x) y))}

(inspect-branch :bob)

{"foo"
 {"f"
  {:name :foo/f,
   :ref "e073c",
   :time #inst "2019-04-16T00:25:03.183-00:00",
   :op :add}}}

(run :alice :foo/f 4)

8

(run :bob :foo/f 4 2)
10

(run :alice :foo/g 3)

8

(def merged (collapse-branches :master :alice :bob))

merged
{:code
 {"c255a" {:form (fn [x] (* 2 x)), :links {}},
  "53d13" {:form (fn [x] (f (inc x))), :links {f "c255a"}},
  "e073c" {:form (fn [x y] (+ (* 2 x) y)), :links {}}},
 :names
 {"foo"
  {"f"
   {:name :foo/f,
    :ref "e073c",
    :time #inst "2019-04-16T00:25:03.183-00:00",
    :op :add},
   "g"
   {:name :foo/g,
    :ref "53d13",
    :time #inst "2019-04-16T00:20:50.991-00:00",
    :op :add}}}}

(run merged :foo/g 3)

8

(run merged :foo/f 2 2)

6

(use-branch :alice)

(inspect :foo/f)

{:sha1 "c255a", :dependencies {}, :form (fn [x] (* 2 x))}

(inspect-branch :alice)

{"foo" {"f" "c255a"
        "g" "53d13"}
 nil {"h" "60830"}}

(run :alice :h 3 4)
14

(:form (inspect :foo/f))
(fn [x] (* 2 x))




(defsn :foo/f
  (fn [x] (* x x)))

{:form (fn [x] (* x x)), :links {}}

(with-versions {f :foo/f}
  (defsn :h
    (fn [x y] (+ (f x) (f y)))))

{:form (fn [x y] (+ (f x) (f y))), :links {f "79f6e"}}

(run :alice :h 3 4)
25

(run :alice :foo/g 3)
8
(inspect :foo/g)

{:sha1 "53d13", :dependencies {f "c255a"}, :form (fn [x] (f (inc x)))}


(inspect  "c255a")
{:sha1 "c255a", :dependencies {}, :form (fn [x] (* 2 x))}
