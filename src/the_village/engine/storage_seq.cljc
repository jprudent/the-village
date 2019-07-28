(ns the-village.engine.storage-seq
  (:require [the-village.engine.storage :as storage]
            [the-village.utils.m :as m]))


(defrecord SeqStorage [goods capacity on-overflow]
  storage/Storage

  (gather [this n]
    (if (< (count goods) n)
      (m/ko this :shortage "not enough goods"
            :number-requested n)
      (m/ok (update this :goods (partial drop n))
            (take n goods))))

  (store [this good]
    (if (= (count goods) capacity)
      (case on-overflow
        :reject
        (m/ko this :overflow "capacity overflow"
              :good-requested good)
        :destroy
        (m/then (storage/gather this 1)
                (storage/store good)))
      (m/ok (update this :goods conj good)))))