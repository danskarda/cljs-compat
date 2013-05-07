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

(defn wrap-ns-dot-macro
  "Middleware for transforming ns :uses and :requires.
   Transforms (:use blahblah.macro) to (:use-macro...)"
  [FN]
  (fn [USE [NS :as FORM]]
    (if (and (ns-macro-map USE)
             (-> NS name (.endsWith ".macro")))
      (FN (ns-macro-map USE) FORM)
      (FN USE FORM))))

(defn wrap-ns-maybe-macro-cljs
  "Middleware which adds -cljs namespaces (if they exists)"
  [FN]
  (fn [USE FORM]
    (map (fn [[ur n :as f]]
           (if (#{:use-macros :require-macros} ur)
             [ur (update-in n [0] maybe-cljs)]
             f))
         (FN USE FORM))))

(defn wrap-ns-clojure-test
  "Middleware for transforming clojure.test to cemerick.cljs.test
   only when there are :refer or :only"
  [FN]
  (fn [USE [NS & REST :as FORM]]
    (if (and (= NS :clojure.test)
             (empty? (filter #{:refer :only} REST)))
      (FN (ns-macro-map USE USE) FORM)
      (FN USE FORM))))

(defn ns-noop
  [USE FORM] [[USE FORM]])

(def ns-default-transformer
  (-> ns-noop
      wrap-ns-maybe-macro-cljs
      wrap-ns-dot-macro
      wrap-ns-clojure-test))

(defn transform-ns-form
  [MW NAME & ARGS]
  (let [docstring (when (string? (first ARGS)) (first ARGS))
        forms     (if docstring (next ARGS) ARGS)

        rebuild   (fn [u forms]
                    (mapcat #(if (coll? %)
                               (MW u %)
                               (MW [%]))
                            forms))

        result    (->> (mapcat (fn [[u & forms :as x]]
                                 (if (#{:use :require} u)
                                   (rebuild u forms)
                                   [x]))
                               forms)
                       (map (fn [[k & r]] {k r}))
                       (apply merge-with concat)
                       (map (fn [[k r]] (cons k r)))
                       seq )]
    (if docstring
      (list* 'ns NAME docstring result)
      (list* 'ns NAME result))))

(def toplevel-map
  {'ns (fn [N & R] (apply transform-ns-form ns-default-transformer N R))})

(defn transform-toplevel
  ([MAP SEQ]
     (map #(if-let [tf (and (coll? %) (get MAP (first %)))]
             (apply tf (next %))
             %)
          SEQ))
  ([SEQ]
     (transform-toplevel toplevel-map SEQ)))

(def type-map
  '{clojure.lang.PersistentArrayMap     cljs.lang/PersistentArrayMap,
    clojure.lang.PersistentHashMap      cljs.lang/PersistentHashMap,
    clojure.lang.PersistentTreeMap      cljs.lang/PersistentTreeMap
    clojure.lang.PersistentHashSet      cljs.lang/PersistentHashSet,
    clojure.lang.PersistentTreeSet      cljs.lang/PersistentTreeSet})

(def namespace-map
  '{clojure.test                        cemerick.clojure.test})

(def expression-map
  {})

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
  ([TOPLEVEL EXPR SOURCE]
     (->> SOURCE
          cbuild/read-forms
          (transform-toplevel TOPLEVEL)
          (map #(walk/postwalk-replace EXPR %))
          cbuild/write-forms))
  ([SOURCE]
     (transmogrify toplevel-map expression-map SOURCE)))
