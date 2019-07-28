(ns the-village.factory)

(def well-config
  {:input-storages  {:unit-of-work {:capacity    0
                                    :on-overflow :reject}
                     :empty-bucket {:capacity    10
                                    :on-overflow :destroy}}
   :output-storages {:filled-bucket {:capacity    10
                                     :on-overflow :reject}}
   :receipe         {:inputs  {:unit-of-work 1
                               :empty-bucket 1}
                     :outputs {:filled-bucket 1}}})

(defrecord Free [])
(defrecord Money [amount])
(defrecord Good [amount])

(defprotocol Factory
  (cook [factory]
    "the factory will try to apply the receipe.
    out: the updated factory")
  (supply [factory input exchanged-good]
    "Supply input good for an exchanged good (for
    free, money, output good)
    out: the updated factory and some goods")
  (drain [factory output exchanged-good]
    "Exchange an output good for something else (for
    free, money, input good"))

(defprotocol Building
  (enter [building villager])
  (leave [building villager]))
