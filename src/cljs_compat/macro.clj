(ns cljs-compat.macro
  "Language dependant macros")

(defmacro in-clojure [BODY]
  BODY)

(defmacro in-clojurescript [BODY]
  nil)

(defmacro in-lang [& {:keys [clojure]}]
  clojure)
