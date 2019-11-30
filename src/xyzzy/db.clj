(ns xyzzy.db
  (:refer-clojure :exclude [intern contains? resolve])
  (:require [xyzzy.datastore :as ds]
            [xyzzy.ihs :as ihs]))

(defonce ^{:private true} BOB (atom {}))

(defn db []
  @BOB)

(defn intern
  "Adds v to the datastore transactionally and returns a unique key which will
  *always* point to v."
  [v]
  (swap! BOB ihs/intern v)
  (ds/ref (ihs/find-key @BOB v)))

(def contains? ihs/contains?)

(defn resolve [m ref]
  (ihs/lookup m (.bytes ref)))
