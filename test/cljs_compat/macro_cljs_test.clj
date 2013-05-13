(ns cljs-compat.macro-cljs-test
  (:use     clojure.test clojure.data)
  (:require [cljs-compat.macro-cljs :as tm]))

(def form-deftype
  '(cljs-compat.macro-cljs/deftype FooBar [X]
     clojure.lang.ILookup
     (valAt [T I] false)
     (valAt [T I D] D)

     Object
     (equals [X Y] false)))

(def sortedseq-deftype
  '(cljs-compat.macro-cljs/deftype SomeSorted []
     Ignored
     (should-be-left-untouched [X] nil)

     clojure.lang.Seqable
     (seq [X] X)

     clojure.lang.Sorted
     (seqFrom [SC KEY ASCENDING?] nil)))

(def form-extend-protocol
  '(cljs-compat.macro-cljs/extend-protocol java.lang.Object
     FooBar
     (toString [x] x)
     (equals [x y] true)
     (bazBar [] 1)

     Bar
     (toString [x] x)
     (equals [x y] false)
     (bazBar [] 2)))

(deftest test-extend-type
  (is (= (macroexpand-1 form-deftype)
         '(clojure.core/deftype FooBar [X]
            IEquiv
            (-equiv [X Y] false)

            cljs.core/ILookup
            (-lookup [T I] false)
            (-lookup [T I D] D))))


  (is (= (macroexpand-1 sortedseq-deftype)
        '(clojure.core/deftype SomeSorted []
            Ignored
            (should-be-left-untouched [X] nil)

            cljs.core/ISeqable
            (-seq [X] X)

            cljs.core/ISorted
            (-sorted-seq [S ASCENDING?] ((if ASCENDING? -seq -rseq) S))
            (-sorted-seq-from [SC KEY ASCENDING?] nil)))))


(deftest test-extend-protocol
  (is (= (->> form-extend-protocol
              macroexpand-1
              rest
              (map macroexpand-1))
         '[(clojure.core/extend-type FooBar
             IEquiv
             (-equiv [x y] true)

             Object
             (bazBar [] 1)
             (toString [x] x))

           (clojure.core/extend-type Bar
             IEquiv
             (-equiv [x y] false)

             Object
             (bazBar [] 2)
             (toString [x] x))])))
