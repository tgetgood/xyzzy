(ns xyzzy.codebase
  (:refer-clojure :exclude [intern])
  (:require [clojure.string :as string]
            [xyzzy.codebase.storage :as store]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Branching
;;;;;
;;;;; Currently there is only one branch, which is to say no branching. That's a
;;;;; problem. but far from my most pressing.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce current-branch
  (atom {}))

(defn- bname [n]
  (str (name n) ".branch"))

(defn get-branch [n]
  (store/branch (bname n)))

(defn get-code [n]
  (store/file-backed-mem-store "all.code"))

(defn- set-branch! [n]
  (reset! current-branch
          {:name n :names (get-branch n) :code (get-code n)}))

(defn use-branch [name]
  (set-branch! name))

(defn fork-branch [from to]
  (reset! current-branch
   {:name to
    :names (store/fork-branch (bname from) (bname to))
    :code (get-code to)}))

(defn combine-branches [& branches]
  {:code (store/as-map (get-code nil))
   :names
   (into {}
         (map (fn [branch] [branch (store/as-map (get-branch branch))]))
         branches)})

(defn collapse-branches [onto & branches]
  {:code  (store/as-map (get-code onto))
   :names (let [onto (get-branch onto)]
            (apply merge-with merge (store/as-map onto)
                   (map store/as-map (map get-branch branches))))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Namespaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn advance-branch [branch sym ref]
  (store/intern (:names branch) (store/ns-sym sym ref)))

(defn ns-map
  "Returns the ns-map of the current branch. The ns map is a map whose keys are
  namespace names (strings) and whose values are maps from var names (again
  strings) to ns entries."
  []
  (store/as-map (:names @current-branch)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Working with codebase images
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commit
  "Update current branch so that sym points to id."
  [sym sha]
  (store/intern (:names @current-branch) (store/ns-sym sym sha)))

(defn lookup
  "Somewhat ad hoc 'lookup thing' fn. ¿DWIM at it's finest?"
  ([x]
   (lookup @current-branch x))
  ([branch x]
   (store/lookup
    ;; FIXME: This looks a lot like lazy polymorphism
    (cond
      (string? x)  (:code branch)
      (keyword? x) (:names branch))
    x))
  ([branch sym time]
   (store/branch-lookup (:names branch) sym time)))

(defn codebase
  "Returns a map of all snippets ever created indexed by id."
  []
  (store/as-map (:code @current-branch)))

(defn qlook [{:keys [branch name]}]
  (:ref (lookup {:names (get-branch branch)} name)))

(defn resolve-links
  "Links must always point to concrete SHAs. If a link is given as an ns
  qualified symbol, then look up the sha that that symbol points to right now."
  [m]
  (into {}
        (map (fn [[k v]]
               [k
                (cond
                  (string? v)  v
                  (keyword? v) (:ref (lookup @current-branch v))
                  (map? v)     (qlook v))])
             m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Snippets
;;
;; Snippets are minimal, meaningful, fragments of code.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-snippet
  "Expects a map with keys :form and :links. Returns the stored snippet matching
  the given code, creating a new entry if necessary."
  [snip]
  (println snip)
  (store/intern (:code @current-branch) snip))

(defmacro snippet
  "Syntactic sugar for writing linked snippets."
  {:style/indent [1]}
  [bindings expr]
  `(create-snippet {:form  '~expr
                    :links '~bindings}))

(def ^:dynamic *deps* {})

(defmacro with-versions [m & body]
  (let [m' (into {} (map (fn [[k v]] `['~k ~v])) m)]
    `(binding [*deps* (merge *deps* (resolve-links ~m'))]
       ~@body)))

(defmacro defsn [name form]
  `(let [snip# (create-snippet {:links *deps* :form '~form})
         hash# (:sha1 (meta snip#))]
     (advance-branch @current-branch ~name hash#)
     snip#))

(defn clean [{:keys [form links] :as o}]
  {:sha1 (:sha1 (meta o))
   :dependencies links
   :form form})

(defn inspect
  ([id]
   (if (string? id)
     (clean (lookup @current-branch id))
     (inspect (:name @current-branch) id)))
  ([branch id]
   (when-let [id (:ref (lookup {:names (get-branch branch)} id))]
     (clean
      (lookup {:code (get-code branch)} id)))))

(defn inspect-branch [name]
  (store/as-map (get-branch name)))

(defn code-store []
  (store/as-map (get-code nil)))


(defn edit
 "Returns code of snippet in a form, which when edited and evalled, will create
  a new snippet."
  [k]
  (let [id (if (string? k) k (:ref (store/lookup (:names @current-branch) k)))
        {:keys [form links]} (store/lookup (:code @current-branch) id)]
    (if (empty? links)
      `(defsn ~k
         ~form)
      `(with-versions ~links
         (defsn ~k
           ~form)))))
