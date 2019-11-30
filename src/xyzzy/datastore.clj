(ns xyzzy.datastore
  (:refer-clojure :exclude [resolve hash ref intern])
  (:require [clojure.edn :as edn]
            [hasch.core :as h]))

(def hex-chars
  (into #{\a \b \c \d \e \f}
        (map (comp first str))
        (range 10)))

(deftype Ref [bytes]
  Object
  (toString [_]
    (str "#ref \"" (h/hash->str bytes) "\"")))

(defn- print-ref
  [^Ref ref ^java.io.Writer w]
  (.write w (.toString ref)))

(defmethod print-method Ref
  [^Ref r ^java.io.Writer w]
  (print-ref r w))

(defmethod print-dup Ref
  [^Ref r ^java.io.Writer w]
  (print-ref r w))

(defn ref [bytes]
  (Ref. bytes))

(defn ref? [x]
  (instance? Ref x))

(defn read-ref [hex]
  {:pre [(string? hex)
         (every? hex-chars hex)]}
  ;; FIXME: I don't like using stings for refs --- strings are only suitable for
  ;; encoding natural language --- but '07bf is not a valid symbol; it's a
  ;; malformed number...
  (ref (map #(Integer/parseInt (apply str %) 16) (partition 2 hex))))

;; REVIEW: What if you want to refer to a constant from another doc? Hashing
;; 31.77 will generally result in bloat. Do we want a lit-ref literal where we
;; can refer to an expression directly by value while keeping provenance info,
;; but without putting it in the global store, or do we want to refer to such
;; quantities by relative queries --- spectre like paths --- into a larger
;; document? Transclusion should not require creating new ddcuments in either
;; case, the link itself should be the thing created. A first class thing. So is
;; the link a document?
(def data-readers
  {'ref read-ref})

(defn hash
  "Hashes here are infinite lazy sequences of bytes as in Bagwell(2001), but
  whereas Bagwell truncates the stream and buckets collisions after a certain
  point, I'm taking as many bytes as necessary to not have a collision. Note:
  this is less performant than Bagwell's optimisation, and requires a hash
  function that is guaranteed to eventually distinguish two non-identical edn
  expressions.
  FIXME: hashes only produce 512 bits at present."
  ;; REVIEW: Would it be enough to just add a salt and rehash each time you need
  ;; another 512 bits?
  [x]
  (h/edn-hash x))

(defn resolve
  "References are destructings of hashes. A hash always points to a value, so
  resolution is always straight forward (if arbitrarily indirect)."
  [store reference]
  (comment "example reference"
           {{{:thing.core/f :x.y/z} :org.me.names} "abc123f..."}
           "will be equivalent to (metaphorically speaking)"
           (-> "abc123"
               (resolve :org.me/names)
               (resolve :x.y/z)
               (resolve :thing.core/f))))



;; REVIEW: A NameMap is just a map with metadata. Should there be a separate API
;; for this or just another data format?
;;
;; This is a deeper question: should I base more or less everything on protocols
;; (data formats) or APIs? When I phrase it like that, it's a no brainer...

(def name-map-format
  {:names {
           ;all names as ns qualified key - value pairs
           }
   :version :? ; this is relative to the publisher...
   :previous [122 31 77 218 '...]
   })

;; A name map is more than a map, it's a sequence of maps related to each other
;; by evolution. It's a value that both changes over time and keeps track of its
;; history. Name maps can branch as well.

(def example-document
  {:content [:doc-markup-tag
             "Some people agree with Edgar when he says: "
             {:reference {:name :poe-quotes.hardcore/amontillado
                          :name-map "abc123..."}}
             ]
   :meta {:hash-table {"abc123..." {:names {:poe-quotes.hardcore/amontillado
                                            "def678..."}
                                    :author {:name "" :pub-key ""}
                                    ;; The author here can track forking. Anyone
                                    ;; can modify anything they can see, so
                                    ;; forking has to be transparent.
                                    :previous "abc122..."}
                       "def678..." "some edn value, including, possibley,
                       another doc, complete with another name-map, references,
                       etc.."}}})

;; Naming name maps is a higher order thing. Basically, the name of a name map
;; is an entry in another name map. Thus if a doc wants to combine names from
;; different name maps in one doc, we need indirect references. Maybe something
;; like:

(def example-rehash-doc
  {:content [:start-here
             {:stuff {:reference {:in {:alice.names.code/experimental "a1"}
                                  :reference {:thing.core/f
                                              :alice.names.code/experimental}}}}]
   :meta {:... '...}})

;; Now, imagine that Alice publishes a new set of names

{:names {:thing.core/f "cc"}
 :author :alice
 :previous "a1"}

;; If you're watching her publication channel --- for lack of a better term ---
;; then a document viewer (the distinction between viewer and editor has to go
;; with the distinction between reader and author), will be able to see that the
;; name-map you've refered to as (named) :alice.names.core/experimental has a
;; new version, and in that new version, the name you're refering, thing.core/f,
;; has changed. The viewer has to provide the option to see the difference that
;; new name will make to everything downstream of where you are now.
;;
;; Recursive, but explicit, propagation of changes is important for evolution
;; without breakage. Well, lack of breakage is impossible in general, but making
;; the recursive changeset explicit means that any breaking changes will break
;; the dev environment immediately, or conversely, if local tests pass, then the
;; same tests will pass in any environment.
;;
;; That's the dream

;; Going back to a previous version of names just means tracing the history
;; chain backwards. Reasoning about who could have meant what, when, requires
;; both timestamps by publishers, and recording the time that we (locally)
;; became aware of the new published content.

;; Documents which contain all referents all the way down, are
;; publishable. Short hashes are unambiguous, because they point to values
;; within the doc. All that's necessary is that as new refs are created, they
;; are long enough to be unambiguous.

;; So what's the point of the global store?


;; When you publish a document, everything is static. No names. What about
;; publishing a namemap? Here, the names correspond directly to concrete values,
;; so the map of names is a value, but the map also contains a history.

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

(defn -main []
  (set! *read-eval* false)
  (set! *default-data-reader-fn* tagged-literal)
  (set! *warn-on-reflection* true)
  (when (not (clojure.core/contains? *data-readers* 'ref))
    (set! *data-readers* (merge *data-readers* data-readers))))
