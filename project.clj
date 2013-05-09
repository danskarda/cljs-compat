(defproject cljs-compat "0.1.0"
  :description "Clojure & ClojureScript compatibility macros"
  :dependencies    [[org.clojure/clojure "1.5.1"]]

  :plugins         [[lein-cljsbuild      "0.3.0"]
                    [lein-webd           "0.1.0"]]

  :hooks           [leiningen.cljsbuild]

  :cljsbuild
  {:builds         {:repl
                    {:jar               true
                     :source-paths      ["src-cljs"]}}}

  :profiles
  {:dev
   ;; explicitely state the cljsbuild so we can test in repl
   {:dependencies  [[cljsbuild          "0.3.0"]]}})
