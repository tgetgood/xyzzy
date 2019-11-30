(ns xyzzy.ihs
  (:refer-clojure :exclude [intern contains?])
  (:require [xyzzy.datastore :as ds]))

(defn value? [m]
  (clojure.core/contains? m :leaf))

(defn grandfathered? [m]
  (clojure.core/contains? m :grandfather))

(defn value [m]
  (cond
    (value? m)         (:value (:leaf m))
    (grandfathered? m) (:grandfather m)))

(defn lookup* [m [h & t]]
  (when (seq m)
    (if h
      (when-let [v (get m h)]
        (recur v t))
      m)))

(defn lookup [m k]
  (let [v (lookup* m k)]
    (cond
      (value? v)         (:value (:leaf v))
      (grandfathered? v) (lookup m (.bytes (:grandfather v))))))

(defn contains* [v m [h & t]]
  (if (value? m)
    (if (= v (value m))
      true
      false)
    (if-let [n (get m h)]
      (recur v n t)
      false)))

(defn contains? [m v]
  (contains* v m (ds/hash v)))

(declare intern*)

(defn shared-prefix [[h1 & t1] [h2 & t2]]
  (if (= h1 h2)
    (cons h1 (shared-prefix t1 t2))
    '()))

(defn add-2 [m [h1 & t1 :as k1] v1 [h2 & t2 :as k2] v2]
  (if (= h1 h2)
    (assoc m h1 (add-2 (get m h1) t1 v1 t2 v2))
    (-> m
        (intern* k1 v1)
        (intern* k2 v2))))

(defn extend-hash [m t v]
  (let [v'     (-> m :leaf :value)
        k'     (ds/hash v')
        prefix (into [] (shared-prefix (ds/hash v) k'))
        t'     (drop (count prefix) k')]
    (-> m
        (dissoc :leaf)
        (assoc :grandfather (ds/ref (conj prefix (first t'))))
        (add-2 t v t' v'))))

(defn intern* [m [h & t] v]
  (if (clojure.core/contains? m h)
    (let [node (get m h)]
      (assoc m h (if (value? node)
                   (extend-hash node t v)
                   (intern* node t  v))))
    (assoc m h {:leaf {:value v
                       :added (java.util.Date.)}})))

(defn intern [m v]
  (if (contains? m v)
    m
    (intern* m (ds/hash v) v)))

(defn find* [m [h & t] k v]
  (if (value? m)
    (when (= (value m) v)
      k)
    (recur (get m h) t (conj k h) v)))

(defn find-key [m v]
  (when (contains? m v)
    (find* m (ds/hash v) [] v)))
