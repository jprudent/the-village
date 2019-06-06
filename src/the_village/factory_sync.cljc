(ns the-village.factory-sync
  (:require [the-village.factory :as factory]
            [the-village.utils :as u]))


(defrecord SeqStorage [vals capacity on-overflow]
  factory/Storage
  (retrieve [this n]
    (if (< (count vals) n)
      [this (ex-info "not enough goods" (assoc this
                                          :failure-type :shortage
                                          :number-requested n))]
      [(update this :vals (partial drop n)) (take n vals)]))
  (store [this good]
    (if (= (count vals) capacity)
      (case on-overflow
        :reject
        [this (ex-info "capacity overflow" (assoc this
                                             :failure-type :overflow
                                             :good-requested good))]
        :destroy
        (-> (factory/retrieve this 1)
            (first)
            (factory/store good)))
      [(update this :vals conj good)])))

(defn input-reader
  [conf input]
  (get-in conf [:inputs input :impl]))

(defn implement-storage
  [conf]
  (reduce
    (update %1 %2 u/map-vals map->SeqStorage)
    conf
    [:inputs :outputs]))

(defrecord SyncFactory [config]
  factory/Factory
  (cook [this]
    ;; get the ingredients
    (let [{:keys [inputs receipe]} config
          receipe-inputs (:inputs receipe)
          (reduce-kv
            (fn [inputs input quantity]
              )
            [inputs {}]
            receipe-inputs )])
    ;;
    ))

(defn ->sync-factory [config]
  (->SyncFactory (implement-storage config)))
