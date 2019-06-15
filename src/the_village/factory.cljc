(ns the-village.factory
  (:require [clojure.core.async :as async]))

(def well-config
  {:input-storage  {:unit-of-work {:capacity    0
                                   :on-overflow :reject}
                    :empty-bucket {:capacity    10
                                   :on-overflow :destroy}}
   :output-storage {:filled-bucket {:capacity    10
                                    :on-overflow :reject}}
   :receipe        {:inputs  {:unit-of-work 1
                              :empty-bucket 1}
                    :outputs {:filled-bucket 1}}})

(defprotocol Factory
  (cook [factory] "the factory will try to apply the
  receipe")
  (exchange-output [factory output exchanged-good]
    "Exchange an output good for something else"))
