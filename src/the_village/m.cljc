(ns the-village.m)

(defrecord R [e v ex])

(defn ok
  ([e] (ok e nil))
  ([e v]
   (->R e v nil)))

(defn ko [e failure-type msg & {:as extra}]
  (->R e nil
       (ex-info msg
                (merge e {:failure-type failure-type} extra))))

(defn ko? [r] (some? (:ex r)))
(def ok? (complement ko?))

(defmacro ->
  "like a -> macro but for results"
  [r body]
  (let [[f# & args#] body]
    `(if (not (ko? ~r))
       (~f# (:e ~r) ~@args#)
       ~r)))