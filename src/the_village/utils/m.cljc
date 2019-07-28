(ns the-village.utils.m
  "LISP cursed monadic library")

(defrecord R [e v ex])

(defn ko? [r] (some? (:ex r)))
(def ok? (complement ko?))

(defn ok
  ([e] (ok e nil))
  ([e v]
   (->R e v nil)))

(defn ko
  ([e ko]
   {:pre [(ko? ko)]}
   (ko (:e ko)
       (-> ko :ex ex-data :failure-type)
       (-> ko :ex ex-message)))
  ([e failure-type msg & {:as extra}]
   (->R e nil
        (ex-info msg
                 (merge e {:failure-type failure-type} extra)))))


(defmacro then
  "like a -> macro but for results"
  [r body]
  (let [[f# & args#] body]
    `(if (not (ko? ~r))
       (~f# (:e ~r) ~@args#)
       ~r)))