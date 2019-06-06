(ns the-village.factory-sync
  (:require [the-village.factory :as factory]
            [the-village.utils :as u]))

(defrecord SeqStorage [vals capacity on-overflow]
  factory/Storage

  (retrieve [this n]
    (if (< (count vals) n)
      [this (ex-info "not enough goods"
                     (assoc this :failure-type :shortage
                                 :number-requested n))]
      [(update this :vals (partial drop n)) (take n vals)]))

  (store [this good]
    (if (= (count vals) capacity)
      (case on-overflow
        :reject
        [this (ex-info "capacity overflow"
                       (assoc this :failure-type :overflow
                                   :good-requested good))]
        :destroy
        (-> (factory/retrieve this 1)
            (first)
            (factory/store good)))
      [(update this :vals conj good)])))

(defn- implement-storage
  [conf]
  (reduce
    (update %1 %2 u/map-vals map->SeqStorage)
    conf
    [:inputs :outputs]))

(defn- retrieve-goods
  [inputs receipe]
  (reduce-kv
    #(update %1 %2 factory/retrieve %3)
    inputs
    receipe))

(def ^:private retrieved? (comp (complement u/ex?) second))

(defn result [receipe goods]
  (:outputs receipe))

(defrecord SyncFactory [inputs outputs receipe]
  factory/Factory
  (cook [this]
    ;; get the ingredients
    (let [maybe-goods (retrieve-goods inputs (:inputs receipe))]
      (if (every? retrieved? retrieve-goods)
        [(assoc this :inputs (u/map-vals maybe-goods first))
         (result receipe (u/map-vals maybe-goods second))]
        [this (ex-info "unable to retrieve some goods"
                       (assoc this :maybe-goods maybe-goods
                                   :failure-type :shortage))]))))

(defn ->sync-factory [config]
  (map->SyncFactory (implement-storage config)))
