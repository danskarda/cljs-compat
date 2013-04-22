(ns cljs-compat.macro
  "Language dependant macros"
  (:refer-clojure :exclude [deftype defrecord extend-type extend-protocol]))

(defmacro in-clojure [BODY]
  BODY)

(defmacro in-clojurescript [BODY]
  nil)

(defmacro in-lang [& {:keys [clojure]}]
  clojure)

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
