(ns the-village.engine.factory-sync
  (:require [the-village.engine.factory :as factory]
            [the-village.engine.storage :as storage]
            [the-village.engine.storage-seq :as storage-seq]
            [the-village.utils.missing :as u]
            [the-village.utils.m :as m]))

;; TODO this code is not tested !!!
(comment
  (defn implement-storage
    "Create SeqStorage where storage are defined
    in the conf"
    [conf]
    (reduce
      #(update %1 %2 u/map-vals storage-seq/map->SeqStorage)
      conf
      [:input-storages :output-storages]))

  (defn- gather-goods
    [input-storages receipe-inputs]
    (reduce-kv
      #(update %1 %2 storage/gather %3)
      input-storages
      receipe-inputs))

  (defn- create-good [good-key] good-key)

  (defn- store-goods
    [output-storages receipe-outputs]
    (reduce-kv
      (fn [acc good-k good-qty]
        (assoc acc
          good-k
          (nth (iterate
                 (fn [output-storage]
                   (m/then output-storage
                           (storage/store (create-good good-k))))
                 (m/ok (get acc good-k)))
               good-qty)))
      output-storages
      receipe-outputs))


  (defprotocol Take
    (take [exchange factory]))

  (extend-protocol Take
    the_village.engine.factory.Free
    (take [free factory] (m/ok factory))

    the_village.engine.factory.Good
    (take [{:keys [kind amount] :as _good}
           {:keys [output-storages] :as factory}]
      (let [result (storage/gather (kind output-storages)
                                   amount)]
        (if (m/ok? result)
          (m/ok (update-in factory
                           [:output-storages kind]
                           (:e result))
                (:v result))
          (m/ko factory :shortage "not enough good"
                :nested result)))))

  (defrecord SyncFactory [input-storages
                          output-storages
                          receipe]
    factory/Factory
    (cook [this]
      (let [input-goods (gather-goods input-storages
                                      (:inputs receipe))]
        (if (every? m/ok? input-goods)
          (let [output-goods (store-goods output-storages
                                          (:outputs receipe))]
            (if (every? m/ok? output-goods)
              (m/ok (-> (assoc this :inputs (u/map-vals input-goods :e))
                        (update :outputs (u/map-vals output-goods :e))))
              (m/ko this :overflow "the factory is full"
                    :maybe-goods input-goods
                    :output-goods output-goods))
            (m/ko this :shortage "not enough good"
                  :maybe-goods input-goods)))))

    (supply [factory input exchanged-good]
      (let [store-result (storage/store (input input-storages)
                                        (create-good input))]
        (if (m/ok? store-result)
          (-> (take exchanged-good factory)
              (m/then (update-in [:input-storages input] (:e
                                                           store-result))))
          (m/ko factory :overflow "the factory is full"
                :cause store-result)))))

  (defn ->sync-factory [config]
    (map->SyncFactory (implement-storage config))))
