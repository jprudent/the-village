(ns the-village.utils.logging)

(defn debug
  [& args]
  (apply prn args))
