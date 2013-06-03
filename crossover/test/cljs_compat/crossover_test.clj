(ns cljs-compat.crossover-test
  (:use clojure.test
        cljs-compat.crossover))

(defn -conservative [STMTS]
  (transform-toplevel toplevel-conservative STMTS))

(defn -progressive [STMTS]
  (transform-toplevel toplevel-progressive STMTS))

(def ns-def-test
  '(ns foo.foo
     (:refer-clojure :exclude [foobar])
     (:use foo
           bar.macro
           [cljs-compat.macro :only [deftype]]
           [baz :only [bahoo]])
     (:require [clojure.test :as tst :refer [deftest]])))

(deftest test-ns-transformer
  (is (= (-conservative [ns-def-test])
         '[[ns foo.foo
            (:refer-clojure :exclude [foobar])
            (:use [foo])
            (:use-macros [bar.macro])
            (:use-macros [cljs-compat.macro-cljs :only [deftype]])
            (:use [baz :only [bahoo]])
            (:require [clojure.test :as tst :refer [deftest]])]]))

  (is (= (-progressive [ns-def-test])
         '[[ns foo.foo
            (:refer-clojure :exclude [foobar])
            (:use [foo])
            (:use-macros [bar.macro])
            (:use-macros [cljs-compat.macro-cljs :only [deftype]])
            (:use [baz :only [bahoo]])
            (:require [cemerick.cljs.test :as tst])
            (:require-macros [cemerick.cljs.test :refer [deftest]])]]))


  (is (= (-progressive '[(ns foo.bar (:require [baz :as b]))])
         '[(ns foo.bar
             (:require [baz :as b])
             (:refer-clojure :exclude [deftype defrecord
                                       extend-type extend-prototype])
             (:require-macros [cljs-compat.macro-cljs
                               :refer [deftype defrecord
                                       extend-type extend-prototype]]))])))
