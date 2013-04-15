(ns cljs-compat.type
  "Compatibility types")

;;; Date
(defn make-date []
  (js/Date.))

;;; queues
(def empty-queue cljs.core.PersistentQueue/EMPTY)
