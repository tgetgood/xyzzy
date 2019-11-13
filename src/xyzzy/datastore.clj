(ns xyzzy.datastore
  (:refer-clojure :exclude [intern contains? resolve])
  (:require [hasch.core :as h]))

(defprotocol DataStore
  (intern [this value]
    "Store a value and return a unique key which can be used to look it up
    again.")
  ;; REVIEW: Can you store nil in the datastore? Nil is a value: specifically
  ;; the unique empty value. Nothing. So probably yes.
  (lookup [this key])
  (contains? [this value]) ;; REVIEW: true/false, or key/nil?
  (permenantly-delete! [this key value]
    "This should only ever be used in the context of secrets that shouldn't have
    been saved, the right to be forgotten, et al.. The store is intended to be
    monotonic. In fact, this method of deletion is probably too easy."))

(defprotocol NameMap
  (tag [this name key]
    "Returns a new NameMap in which `name` refers to `key`.")
  (untag [this name]
    "Returns a new NameMap in which `name` no longer resolves.")
  (resolve [this name]
    "Return the key to which `name` refers.")
  (as-of [this time-or-version-id]
    "Returns this NameMap is it looked at a previous time.")
  (version [this]
    "Returns a serial version ID which can be used to time travel back to
    now."))

;; A document is a map (a datum) containing keys :content, and :meta. :meta, in
;; turn contains references to the document from which the current one was
;; derived and the edit which made the transition. Editing metadata should give
;; the same audit trail as editing content.

;; So do we need a protocol for documents? Maybe. I'm not sure that's the way to
;; go though. More like a spec.

;; Code, in turn, is a document with a specific structure that defines the
;; environment in which it is to be interpreted. `(inc 4)` is not code without
;; the implicit context of the clojure (verison x) compiler, runtime, etc..

;; The above is all well and good in a local (single machine, or more
;; specifically single thread of control for the store and all name maps)
;; context, now how do we share data?

;; Data cannot just be shared. To be distributed, it must be
;; published. Publication is transactional in nature, and all publications are
;; derived from previous (possibly empty) publications.

;; In particular, that means that a someone can read something published by
;; author A, and publish a modification that derives from A's
;; publication. Publications need to be signed by the author, and possibly by
;; domain certificates as well if we really want to establish origin.

;; But from a particular author, all publications form a sequence. Publishing a
;; new document involves adding a new node to the publication chain which refers
;; to the new doc.

;; Unpublishing is basically impossible. Retractions can be issued. A publisher
;; can stop distributing a document. But you can't force everyone who's ever
;; received a copy to delete it any more than you can recall a printed book and
;; expect everyone to return it. This is important to protect against automated
;; censorship.

;; If a publisher asks that a document be deleted, it's up to each person who
;; has a copy of that doc to decide. It has to be that way.

(defn mem-datastore [init]
  (let [data (atom init)]
    (reify DataStore
      (intern [this value]
        (let [hash (h/edn-hash value)]
          ())))))
