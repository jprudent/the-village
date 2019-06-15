(ns the-village.factory-async
  (require [the-village.factory :as factory]
           [clojure.core.async :as async]))


(comment
  (defn mk-chan
    [{:keys [stock] :as input}]
    (assoc input :chan (async/chan stock)))

  (defn input-ch
    [factory input]
    (get-in factory [:inputs input :chan]))

  (defn <!-input
    [factory input]
    (async/<! (input-ch factory input)))

  (defn cook
    [{:keys [inputs outputs :as receipe]}
     inputs])

  (defn stock-outputs [x])

  (defn ->async-factory
    [config]
    (let [factory (->> config
                       (update-in [:inputs :unit-of-work] mk-chan)
                       (update-in [:inputs :empty-bucket] mk-chan)
                       (update-in [:outputs :filled-bucket] mk-chan))]
      (reify factory/Factory
        (tick [factory]
          (async/go-loop []
            (stock-outputs
              (cook (:receipe config)
                    {:uow          (<!-input factory :unit-of-work)
                     :empty-bucket (<!-input factory :empty-bucket)})))
          factory)
        (exchange-output [_ output exchanged])
        )
      factory)))