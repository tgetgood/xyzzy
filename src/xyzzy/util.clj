(ns xyzzy.util
  (:require [taoensso.timbre :as log]))

(defn vmap
  "Applies f to each value in map m, returning a map with the same keys, and
  transformed values."
  [f m]
  ;; REVIEW: (juxt key (comp f val)) is more transparent than
  ;; (fn [[k v]] [k (f v)]), but it's also less clear. I have to think more
  ;; about the downsides of lambdas.
  (into {} (map (fn [[k v]] [k (f v)])) m))

(def logfile
  "What do you think it is?"
  "events.log")

(def logfile-appender
  {:enabled? true
   :async? true
   :min-level :debug
   :rate-limit [[100 1000]]
   :output-fn identity
   :fn (fn [x]
         (let [data (first (:vargs x))
               meta (select-keys x [:instant :level])]
           (spit logfile (str {:message data :metadata meta} "\n")
                 :append true)))})

(log/set-config!
 {:level :debug
  :timestamp-opts log/default-timestamp-opts
  :middleware []
  :output-fn identity
  :appenders {:file-appender logfile-appender}})

(defn log
  ([level data]
   (log/log level data))
  ([level msg & msgs]
   (log level (apply str msg (interleave (repeat " ") msgs)))))
