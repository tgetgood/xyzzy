(ns xyzzy.document
  (:require [xyzzy.db :as db]))

(def empty-doc
  {:meta    nil
   :content nil})

(def document-format
  {:meta {}
   :content #ref "abc123"})
