(ns the-village.engine.storage-seq-test
  (:require
    #?(:clj  [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is run-tests]])
    [the-village.utils.m :as m]
    [the-village.engine.storage :as storage]
    [the-village.engine.storage-seq :as sut]))

(deftest SeqStorage-test
  (is (= (m/ok (sut/->SeqStorage [:weed] 1 :reject))
         (storage/store (sut/->SeqStorage [] 1 :reject) :weed)))

  (let [full   (sut/->SeqStorage [:x] 1 :reject)
        actual (storage/store full :y)]
    (is (= full (:e actual)))
    (is (m/ko? actual)))

  (let [full   (sut/->SeqStorage [:x :y] 2 :destroy)
        actual (storage/store full :z)]
    (is (= [:z :y] (-> actual :e :goods)))
    (is (m/ok? actual)))

  (is (= (m/ok (sut/->SeqStorage [] 1 :reject) [:weed])
         (storage/gather (sut/->SeqStorage [:weed] 1 :reject) 1)))

  (let [stock  (sut/->SeqStorage [:weed] 2 :reject)
        actual (storage/gather stock 2)]
    (is (= stock (:e actual)))
    (is (m/ko? actual)))

  (let [empty  (sut/->SeqStorage [] 2 :reject)
        actual (-> (sut/->SeqStorage [] 2 :reject)
                   (storage/gather 1)
                   (m/then (storage/store :x))
                   (m/then (storage/store :y)))]
    (is (= empty (:e actual)))
    (is (m/ko? actual)))

  (let [actual (-> (sut/->SeqStorage [] 2 :reject)
                   (storage/store :x)
                   (m/then (storage/store :y))
                   (m/then (storage/gather 1)))]
    (is (= (m/ok (sut/->SeqStorage [:y] 2 :reject) [:x])
           actual))))
