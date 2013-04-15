(ns cljs-compat.type
  "Compatibility types")

(defn now []
  (java.util.Date.))

(def empty-queue clojure.lang.PersistentQueue/EMPTY)
