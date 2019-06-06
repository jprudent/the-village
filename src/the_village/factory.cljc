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

(defprotocol Storage
  (retrieve [storage n]
    "retrieve n goods from storage.
    IN : - n : the number of good to be retrieved
    OUT : - success : [storage-updated goods]
          - failure : [storage-updated failure]")
  (store [storage good]
    "stock a good.
    IN : - good : the good to store
    OUT : - success : [storage-updated]
          - failure : [storage-updated failure]"))

(defprotocol Factory
  (cook [factory] "the factory will try to produce
  something")
  (exchange-output [factory output exchanged-good]
    "Exchange an output good for something else"))
