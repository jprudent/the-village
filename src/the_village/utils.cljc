(ns the-village.utils
  "pure Clojure functions that are missing a built in")

(defn map-vals
  "apply f to values of map"
  [m f]
  {:pre [(map? m)]}
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn ex? [e] (instance? Exception e))