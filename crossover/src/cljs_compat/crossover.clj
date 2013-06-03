(ns cljs-compat.crossover
  "Provide a cljsbuild transformers"
  (:require [clojure.walk         :as walk]
            [cljsbuild.crossover :as cbuild]))

(defn maybe-cljs
  "Returns clojurescript variant, if exists"
  [X]
  (try (let [xc (-> X name (str "-cljs") symbol)]
         (require xc)
         xc)
       (catch Exception _
         X)))

(def ns-macro-map
  {:use :use-macros, :require :require-macros})

(defn ns-use-dot-macro
  "Transforms [:use [foo.bar.macro]] into [:use-macro ...]
   For all namespaces ending with .macro. Same for require macros"
  [[USE [NS & _ :as SPEC] :as FORM]]
  (if (and (ns-macro-map USE)
           (-> NS name (.endsWith ".macro")))
    [(ns-macro-map USE) SPEC]
    FORM))

(defn ns-maybe-macro-cljs
  "If USE is :use-macros, adds -cljs suffix to namespaces (if ns exists).
   Same for :require-macros"
  [[USE SPECS :as FORM]]
  (if (#{:use-macros :require-macros} USE)
    [USE (update-in SPECS [0] maybe-cljs)]
    FORM))

(defn ns-clojure-test
  "Middleware for transforming clojure.test to cemerick.cljs.test
   only when there are :refer or :only"
  [[USE [NS & REST :as SPEC] :as FORM]]
  (if (= NS 'clojure.test)
    (let [[& {:keys [as refer only]}] REST
          n  'cemerick.cljs.test]
      (concat
       (if as
         [[:require [n :as as]]]
         [[:require [n]]])
       (when (and only (= :use USE))
         [[:use-macros [n :only only]]])
       (when (and refer (= :require USE))
         [[:require-macros [n :refer refer]]])))
    [FORM]))

(defn ns-make-safe-fn
  [FN IDENTITY]
  (fn [[_ SPEC :as FORM]]
    (if (coll? SPEC)
      (FN FORM)
      (IDENTITY FORM))))

(defn wrap-ns-map [FN]
  (fn [SPECS] (map (ns-make-safe-fn FN identity) SPECS)))

(defn wrap-ns-mapcat [FN]
  (fn [SPECS] (mapcat (ns-make-safe-fn FN vector) SPECS)))

(defn ns-inject-cljs-compat
  "Injects cljs-compat.macros compatibility if it is not already there"
  [SPECS]
  (if (some (fn [[_ SPEC]]
              (and (coll? SPEC)
                   ('#{cljs-compat.macro cljs-compat.macro-cljs} (first SPEC))))
            SPECS)
    SPECS
    (let [defs '[deftype defrecord extend-type extend-prototype]]
      (concat SPECS
              [[:refer-clojure  :exclude defs]
               [:require-macros ['cljs-compat.macro-cljs :refer defs]]]))))

(def ns-conservative-transformer
  (comp (wrap-ns-map ns-maybe-macro-cljs)
        (wrap-ns-map ns-use-dot-macro)))

(def ns-progressive-transformer
  (comp ns-inject-cljs-compat
        (wrap-ns-mapcat ns-clojure-test)
        ns-conservative-transformer))

(require '[clojure.pprint :as pprint])

(defn transform-ns-form
  ([MW] ;; curry
     (fn [& REST] (apply transform-ns-form MW REST)))
  ([MW NAME & ARGS]
     (let [docstring (when (string? (first ARGS)) (first ARGS))
           forms     (if docstring (next ARGS) ARGS)

           forms     (reduce
                      (fn [result [k & parts :as spec]]
                        (into result
                              (if (#{:use :require :use-macros :require-macros} k)
                                (map #(if (coll? %) [k %] [k [%]]) parts)
                                [spec])))
                      [] forms)

           result     (->> (MW forms)
                           (map (fn [[k & r]] {k r}))
                           (apply merge-with concat)
                           (map (fn [[k r]] (cons k r)))
                           seq )]
       (if docstring
         (list* 'ns NAME docstring result)
         (list* 'ns NAME result)))))

(def toplevel-conservative
  {'ns (transform-ns-form ns-conservative-transformer)})

(def toplevel-progressive
  {'ns (transform-ns-form ns-progressive-transformer)})

(defn transform-toplevel
  [MAP SEQ]
  (map #(if-let [tf (and (coll? %) (get MAP (first %)))]
          (apply tf (next %))
          %)
       SEQ))

(def type-map
  '{clojure.lang.PersistentArrayMap     cljs.lang/PersistentArrayMap,
    clojure.lang.PersistentHashMap      cljs.lang/PersistentHashMap,
    clojure.lang.PersistentTreeMap      cljs.lang/PersistentTreeMap
    clojure.lang.PersistentHashSet      cljs.lang/PersistentHashSet,
    clojure.lang.PersistentTreeSet      cljs.lang/PersistentTreeSet
    clojure.lang.PersistentQueue        cljs.lang/PersistentQueue})

(def expression-map
  '{java.lang.Exception                 js/Error,
    java.lang.AssertionError            js/Error,

    clojure.lang.MapEntry.              vector
    clojure.lang.PersistentQueue/EMPTY  cljs.core.PersistentQueue/EMPTY

    java.util.Date                      js/Date
    java.util.Date.                     js/Date.})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;     Transmogrify
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transmogrify
  "Transform input SOURCE code using TOPLEVEL and EXPR map.

   For all toplevel forms, transform calls FN from TOPLEVEL MAP
   {symbol fn}

   For all expressions (either quoted or unquoted) transformogrify
   tries remap using EXPR map"
  [TOPLEVEL EXPR SOURCE]
  (->> (if (string? SOURCE)
         (-> SOURCE cbuild/remove-cljsbuild-comments cbuild/read-forms)
         SOURCE)
       (transform-toplevel TOPLEVEL)
       (map #(walk/postwalk-replace EXPR %))
       cbuild/write-forms))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;     Crossovers
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn conservative
  "Apply conservative transformation on source code:

    1) transform use/require to use-require macros for namespaces which
       ends with .macro

    2) add -cljs suffix for :use-macros iff namespace exists"
  [SOURCE]
  (transmogrify toplevel-conservative {} SOURCE))

(defn progressive
  "Apply all transformation on source code:

    1) transform use/require to use-require macros for namespaces which
       ends with .macro

    2) add -cljs suffix for :use-macros iff namespace exists

    3) inject cljs-compat.macros if it is not already used

    4) transform clojure.test into cemerick.cljs.test

    5) transform basic types, exceptions and tests"
  [SOURCE]
  (transmogrify toplevel-progressive (merge type-map expression-map) SOURCE))
