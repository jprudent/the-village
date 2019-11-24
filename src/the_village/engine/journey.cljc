(ns the-village.engine.journey)

(defrecord Journey
  [;; at which time this journey started ?
   started-time
   ;; the precomputed path to complete this journey
   path])

(defn build [{:keys [time-provider] :as _conf} path]
  {:pre [(some? path)]}
  (map->Journey {:started-time (time-provider)
                 :path path}))

(defn step-traveled
  "Update the journey when a step of the path has been
  traveled"
  [journey time-provider]
  (-> journey
      (update :path rest)
      (update :started-time (time-provider))))