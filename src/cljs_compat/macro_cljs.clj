(ns cljs-compat.macro-cljs
  "CLJS Compatibility macros. in-lang and macros for type compatibility (deftype)"
  (:refer-clojure :exclude [deftype defrecord extend-type extend-protocol])
  (:require [cljs-compat.protocols :as proto]))

;;; in-lang macros

(defmacro in-clj [& BODY]
  nil)

(defmacro in-clojure [& BODY]
  nil)

(defmacro in-cljs [& BODY]
  `(do ~@BODY))

(defmacro in-clojurescript [& BODY]
  `(do ~@BODY))

(defmacro in-lang [& {:keys [cljs clojurescript]}]
  (or cljs clojurescript))

;;; type macros

(defn translate [PROTO [NAME & REST :as ORIG]]
  (let [{:keys [method protocol append]} (proto/transmogrify PROTO NAME)]
    (concat (if (and protocol method)
              [[protocol (cons method REST)]]
              [[PROTO ORIG]])
            append)))

(defn zip-headers
  [HEADER PRED? [H & REST :as SEQ]]
  (when SEQ
    (if (PRED? H)
      (recur H PRED? REST)
      (cons [HEADER H]
            (lazy-seq (zip-headers HEADER PRED? REST))))))

(defn translate-type
  [SPECS]
  (->> (zip-headers nil symbol? SPECS)
       (mapcat #(apply translate %))
       (group-by first)
       (sort-by key)
       (mapcat (fn [[proto specs]]
                 (when specs
                   (->> (map second specs)
                        (sort-by first)
                        (cons proto)))))))

(defmacro deftype [NAME FIELDS & REST]
  `(clojure.core/deftype ~NAME ~FIELDS
     ~@(translate-type REST)))

(defmacro defrecord [NAME FIELDS & REST]
  `(clojure.core/defrecord ~NAME ~FIELDS
     ~@(translate-type REST)))

(defmacro extend-type [NAME & REST]
  `(clojure.core/extend-type ~NAME
     ~@(translate-type REST)))

(defmacro extend-protocol [NAME & REST]
  (let [types  (->> (zip-headers nil symbol? REST)
                    (group-by first))
        decls  (map (fn [[t v]]
                      (->> (map second v)
                           (list* 'cljs-compat.macro-cljs/extend-type t NAME)))
                    types)]
    `(do ~@decls)))
