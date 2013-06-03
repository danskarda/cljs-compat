(ns cljs-compat.macro
  "Language dependant macros"
  (:refer-clojure :exclude [deftype defrecord extend-type extend-protocol]))

(defmacro in-clj [& BODY]
  `(do ~@BODY))

(defmacro in-clojure [& BODY]
  `(do ~@BODY))

(defmacro in-cljs [& BODY]
  nil)

(defmacro in-clojurescript [& BODY]
  nil)

(defmacro in-lang [& {:keys [clj clojure]}]
  (or clj clojure))

(defmacro deftype
  [NAME FIELDS & OPTS+SPECS]
  `(clojure.core/deftype ~NAME ~FIELDS ~@OPTS+SPECS))

(defmacro defrecord
  [NAME FIELDS & OPTS+SPECS]
  `(clojure.core/defrecord ~NAME ~FIELDS ~@OPTS+SPECS))

(defmacro extend-type
  [TYPE & SPECS]
  `(clojure.core/extend-type ~TYPE ~@SPECS))

(defmacro extend-protocol
  [PROTO & SPECS]
  `(clojure.core/extend-protocol ~PROTO ~@SPECS))
