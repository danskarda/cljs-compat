(ns cljs-compat.macro-cljs
  "CLJS Compatibility macros. in-lang and macros for type compatibility (deftype)")

;;; in-lang macros

(defmacro in-clj [BODY]
  nil)

(defmacro in-clojure [BODY]
  nil)

(defmacro in-cljs [BODY]
  BODY)

(defmacro in-clojurescript [BODY]
  BODY)

(defmacro in-lang [& {:keys [cljs clojurescript]}]
  (or cljs clojurescript))

;;; type macros

(defn simple-transformer
  [& {:as MAP}]
  (fn [_ [NAME & REST]]
    (cons (get MAP NAME NAME) REST)))

(def object-map
  {:protocol       'Object,
   :methods       {'equals           {:protocol 'IEquiv
                                      :method   '-equiv}}})

(def protocol-map
  {'Object           object-map
   'java.lang.Object object-map

   'clojure.lang.ILookup
   {:protocol       'ILookup
    :methods        {'valAt           {:method   '-lookup}}}})

(defn translate [PROTO [NAME & REST]]
  (let [proto   (protocol-map PROTO)
        meth    (-> proto :methods (get NAME))]
    [(or (:protocol meth) (:protocol proto) PROTO)
     (cons (or (:method meth) NAME) REST)]))

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
       (map #(apply translate %))
       (group-by first)
       (mapcat (fn [[proto specs]]
                 (when specs
                   (cons proto (map second specs)))))))

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
                           (list* 'dx.lang.macro-cljs/extend-type t NAME)))
                    types)]
    `(do ~@decls)))
