(ns cljs-compat.crossover-test
  (:use clojure.test
        cljs-compat.crossover))

(deftest test-ns-transformer
  (is (= (transform-ns '[(ns foo.foo
                             (:use foo
                                   bar.macro
                                   [cljs-compat.macro :only [deftype]]
                                   [baz :only [bahoo]]))])
         '[(ns foo.foo
             (:use-macros      bar.macro
                               [cljs-compat.macro-cljs :only [deftype]])
             (:use             foo
                               [baz :only [bahoo]]))])))
