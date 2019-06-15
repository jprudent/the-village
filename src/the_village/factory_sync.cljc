(ns the-village.factory-sync
  (:require [the-village.factory :as factory]
            [the-village.storage :as storage]
            [the-village.utils :as u]
            [the-village.m :as m]))

(defrecord SeqStorage [vals capacity on-overflow]
  storage/Storage

  (gather [this n]
    (if (< (count vals) n)
      (m/ko this :shortage "not enough goods"
            :number-requested n)
      (m/ok (update this :vals (partial drop n))
            (take n vals))))

  (store [this good]
    (if (= (count vals) capacity)
      (case on-overflow
        :reject
        (m/ko this :overflow "capacity overflow"
              :good-requested good)
        :destroy
        (-> (storage/gather this 1)
            (m/-> (storage/store good))))
      (m/ok (update this :vals conj good)))))

(defn- implement-storage
  [conf]
  (reduce-kv
    #(update %1 %2 u/map-vals map->SeqStorage)
    conf
    [:inputs :outputs]))

(defn- gather-goods
  [inputs receipe]
  (reduce-kv
    #(update %1 %2 storage/gather %3)
    inputs
    receipe))

(def ^:private retrieved? (comp (complement u/ex?) second))

(defn result [receipe goods]
  (:outputs receipe))

(defrecord SyncFactory [inputs outputs receipe]
  factory/Factory
  (cook [this]
    ;; get the ingredients
    (let [maybe-goods (gather-goods inputs (:inputs receipe))]
      (if (every? retrieved? gather-goods)
        [(assoc this :inputs (u/map-vals maybe-goods first))
         (result receipe (u/map-vals maybe-goods second))]
        [this (ex-info "unable to retrieve some goods"
                       (assoc this :maybe-goods maybe-goods
                                   :failure-type :shortage))]))))

(defn ->sync-factory [config]
  (map->SyncFactory (implement-storage config)))
