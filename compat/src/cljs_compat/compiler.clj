(ns cljs-compat.compiler
  "A code to detect if we are inside ordinary Clojure or macro file
   invoked by ClojureScript compiler")

(defn cljs-compiler?
  "Return true if we are inside CLJS compiler"
  []
  (some-> (resolve 'cljs.analyzer/*cljs-file*)
          deref
          (and true)))
