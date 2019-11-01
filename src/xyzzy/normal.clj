(ns xyzzy.normal
  (:require [riddley.walk :as walk]))

(declare walk)

(defn update-bindings [bindings args]
  (reduce (fn [bindings k]
            (assoc bindings k (gensym)))
          bindings args))

(defn pred [bindings]
  (fn [form]
    (or (contains? bindings form)
        (and (seq? form) (contains? #{'fn 'let} (first form))))))

(defn handler [bindings]
  (fn [form]
    (if (contains? bindings form)
      (get bindings form)
      (let [[sf args & body] form
            b' (update-bindings bindings args)]
        `(~sf ~(into [] (map #(get b' %)) args) ~@(map (walk b') body))))))

(defn walk [bindings]
  (partial walk/walk-exprs
           (pred bindings)
           (handler bindings)
           (constantly true)))
