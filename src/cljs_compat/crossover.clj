(ns cljs-compat.crossover
  "Provide a cljsbuild transformers")

(defn transform-ns-form
  [NAME & ARGS]
  (let [docstring (when (string? (first ARGS)) (first ARGS))
        forms     (if docstring (next ARGS) ARGS)

        formmap   (->> (group-by first forms)
                       (reduce-kv #(assoc %1 %2 (apply concat (map rest %3))) {}))

        compat?   #(or (= % 'cljs-compat.macro)
                       (and (coll? %) (= (first %) 'cljs-compat.macro)))

        fix       #(if (= % 'cljs-compat.macro)
                     '[cljs-compat.macro-cljs :only [in-clj in-cljs
                                                     in-cljs in-clojurescript
                                                     deftype defrecord
                                                     extend-type extend-protocol]]
                     (cons 'cljs-compat.macro-cljs
                           (rest %)))

        fixmap    (fn [m from to]
                    (let [found (->> (get m from) (filter compat?))]
                      (if (empty? found)
                        m
                        (-> (update-in m [from] #(remove compat? %))
                            (update-in [to] conj (-> found first fix))))))

        formmap   (-> (fixmap formmap :use :use-macros)
                      (fixmap :require :require-macros))

        result    (map #(apply cons %) formmap)]

    (if docstring
      (list* 'ns NAME docstring result)
      (list* 'ns NAME result))))

(def transform-map
  {'ns transform-ns-form})

(defn transform-ns
  ([SEQ MAP]
     (map #(if-let [tf (and (coll? %) (get MAP (first %)))]
             (apply tf (next %))
             %)
          SEQ))
  ([SEQ]
     (transform-ns SEQ transform-map)))
