(ns cljs-compat.crossover
  "Provide a cljsbuild transformers")

(defn dot-macro?
  "Returns if namespace ends with .macro"
  [X]
  (-> X name (.endsWith ".macro")))

(defn maybe-cljs
  "Returns clojurescript variant, if exists"
  [X]
  (try (let [xc (-> X name (str "-cljs") symbol)]
         (require xc)
         xc)
       (catch Exception _
         X)))

(defn transform-ns-form
  [NAME & ARGS]
  (let [docstring (when (string? (first ARGS)) (first ARGS))
        forms     (if docstring (next ARGS) ARGS)

        formmap   (->> (group-by first forms)
                       (reduce-kv #(assoc %1 %2 (apply concat (map rest %3))) {}))

        macro?   #(if (coll? %)
                    (-> % first dot-macro?)
                    (dot-macro? %))

        fix-it    #(if (coll? %)
                     (cons (-> % first maybe-cljs)
                           (rest %))
                     (maybe-cljs %))

        fixmap    (fn [m from to]
                    (let [found (->> (get m from) (filter macro?))]
                      (if (empty? found)
                        m
                        (-> (update-in m [from] #(remove macro? %))
                            (update-in [to] concat (map fix-it found))))))

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
