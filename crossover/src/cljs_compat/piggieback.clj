(ns cljs-compat.piggieback
  "Integrate crossovers into piggieback"
  (:require [cljsbuild.piggieback   :as p]
            [cljs-compat.crossover  :as cr]
            [clojure.tools.nrepl.middleware :refer [set-descriptor!]]))

(def wrap-conservative-repl (p/make-wrapper cr/conservative))
(def wrap-progressive-repl  (p/make-wrapper cr/progressive))

(set-descriptor! #'wrap-conservative-repl
   {:requires #{"clone"}
    :expects #{"load-file" "eval"}
    :handles {}})

(set-descriptor! #'wrap-progressive-repl
   {:requires #{"clone"}
    :expects #{"load-file" "eval"}
    :handles {}})
