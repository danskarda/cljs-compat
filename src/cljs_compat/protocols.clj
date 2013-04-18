(ns cljs-compat.protocols
  "Map from Clojure to ClojureScript protocols")

(declare protocol-map protocol-method-map)

(defn transmogrify
  "Based on PROTOCOL and METHOD returns map of:

    :protocol   target protocol
    :method     target method
    :append     additional compatibility code to append to extend-type or
                 deftype / defrecord"
  ([PROTOCOL METHOD]
     (transmogrify protocol-map protocol-method-map PROTOCOL METHOD))

  ([PROTO-MAP METHOD-MAP PROTOCOL METHOD]
     (let [proto        (or (PROTO-MAP PROTOCOL) PROTOCOL)
           method       (or (get-in METHOD-MAP [PROTOCOL METHOD])
                            (when (PROTO-MAP PROTOCOL) METHOD))]
       (when method
         (merge {:method method :protocol proto}
                (cond
                 (map? method)    method
                 (symbol? method) {:method method}
                 (coll? method)   (zipmap [:protocol :method] method)))))))

;;; definitions
(defn- clojure-lang
  [SEQ]
  (->> SEQ
       (map #(let [[clj cljs] (if (coll? %) % [% %])]
               (vector (-> (str "clojure.lang." clj) symbol)
                       (-> (str "cljs.core." cljs)   symbol))))
       (into {})))

(def protocol-map
  (merge (clojure-lang '[IFn
                         [Counted ICounted]
                         ; IEmptyableCollection
                         [IPersistentCollection ICollection]
                         [Indexed IIndexed]
                         ISeq
                         ; INext
                         ILookup
                         [Associative IAssociative]
                         [IPersistentMap IMap]
                         IMapEntry
                         [IPersistentSet ISet]
                         [IPersistentStack IStack]
                         [IPersistentVector IVector]
                         IDeref
                         IDerefWithTimeout
                         IMeta
                         ;; IWithMeta
                         IReduce
                         IKVReduce
                         ;; IEquiv
                         IHash
                         [Seqable ISeqable]
                         ;; ISequential
                         [Reversible IReversible]
                         [Sorted ISorted]
                         ;; I do not expect to have cross-plafrom IO
                         ;; IWriter
                         IPending
                         ;; Neither IRef (watches, validators, ...)
                         ;; [IRef IWatchable]
                         IEditableCollection
                         ITransientCollection
                         ITransientAsociative
                         ITransientMap
                         ITransientVector
                         ITransientSet
                         IComparable])                  ; todo

         '{Object                              Object,
           java.lang.Object                    Object

           clojure.core.protocols.CollReduce            cljs.core.IReduce
           clojure.core.protocols.InternalReduce        cljs.core.IReduce
           clojure.core.protocols.IKVReduce             cljs.core.IKVReduce}))

(def protocol-method-map
  '{java.lang.Object
    {equals                     [IEquiv -equiv]
     hashCode                   [IHash  -hash]
     compareTo                  [IComparable -compare]}

    Object
    {equals                     [IEquiv -equiv]
     hashCode                   [IHash  -hash]
     compareTo                  [IComparable -compare]}

    clojure.lang.Counted
    (count                      -icount)

    clojure.lang.IFn
    (invoke                     -invoke)

    clojure.lang.ILookup
    {valAt                      -lookup}

    clojure.lang.ISeq
    {first                      -first
     next                       [INext -next]
     more                       -rest
     cons                       {:method nil}}

    clojure.lang.IPersistentCollection
    {count                      [ICounted -count]
     cons                       -conj
     empty                      [IEmptyableCollection -empty]
     equiv                      [IEquiv -equiv]}

    clojure.lang.Seqable
    {seq                        -seq}

    clojure.lang.Indexed
    {nth                        -nth}

    clojure.lang.Associative
    {containsKey                -contains-key
     entryAt                    {:method nil}
     assoc                      -assoc}

    clojure.lang.IPersistentMap
    {assoc                      -assoc
     assocEx                    {:method nil}
     without                    -dissoc}

    clojure.lang.IMapEntry
    {key                        -key
     val                        -val}

    clojure.lang.IPersistentSet
    {disjoin                    -disjoin
     contains                   [IAssociative -contains-key?]
     get                        [ILookup -lookup]}

    clojure.lang.IPersistentStack
    {peek                       -peek
     pop                        -pop}

    clojure.lang.IPersistentVector
    {length                     [ICounted -count]
     assocN                     -assoc-n
     cons                       [ICollection -conj]}

    clojure.lang.IDeref
    {deref                      -deref}

    clojure.lang.IDerefWithTimeout
    {deref                      -deref-with-timeout}

    clojure.lang.IMeta
    {meta                       -meta}

    clojure.lang.IObj
    {withMeta                   [IWithMeta -with-meta]}

    clojure.lang.IReduce
    {reduce                     -reduce}

    clojure.core.protocols.IKVReduce
    {kv-reduce                  -kv-reduce}

    clojure.core.protocols.CollReduce
    {coll-reduce                -reduce}

    clojure.core.protocols.InternalReduce
    {internal-reduce            -reduce}

    clojure.lang.Reversible
    {rseq                       -rseq}

    clojure.lang.Sorted
    {comparator                 -comparator
     entryKey                   -entry-key
     seqFrom                    {:method                -sorted-seq-from
                                 :append                [[cljs.core.ISorted
                                                          (-sorted-seq [S ASCENDING?]
                                                            ((if ASCENDING? -seq -rseq) S))]]}}

    clojure.lang.IPending
    {isRealized                 -realized?}

    clojure.lang.IEditableCollection
    {asTransient                -as-transient}

    clojure.lang.ITransientCollection
    {conj                       -conj!
     persistent                 -persistent!}

    clojure.lang.ITransientAssociative
    {assoc                      -assoc!}

    clojure.lang.ITransientMap
    {assoc                      [ITransientAssociative -assoc!]
     without                     -dissoc!
     persistent                 [ITransientCollection -persistent!]}

    clojure.lang.ITransientVector
    {assocN                     -assoc-n!
     pop                        -pop!}

    clojure.lang.ITransientSet
    {disjoin                    -disjoin!
     contains                   [IAssociative -contains-key?]
     get                        {:method nil}}})
