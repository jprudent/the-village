(ns the-village.engine.factory-sync-test
  (:require
    #?(:clj  [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is run-tests]])
    [the-village.engine.factory-sync :as sut]
    [the-village.engine.factory :as factory]
    [the-village.utils.m :as m]
    [the-village.engine.storage :as storage]))

(deftest SeqStorage-test
  (is (= (m/ok (sut/->SeqStorage [:weed] 1 :reject))
         (storage/store (sut/->SeqStorage [] 1 :reject) :weed)))

  (let [full   (sut/->SeqStorage [:x] 1 :reject)
        actual (storage/store full :y)]
    (is (= full (:e actual)))
    (is (m/ko? actual)))

  (let [full   (sut/->SeqStorage [:x :y] 2 :destroy)
        actual (storage/store full :z)]
    (is (= [:z :y] (-> actual :e :vals)))
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

(deftest FactorySync-test
  #_(testing "shortage"
    (let [result (-> (sut/->sync-factory factory/well-config)
                     (factory/cook))]
      (is (m/ko? result))
      (is (not (m/ok? result)))
      (is (= :shortage (-> result :ex ex-data :failure-type)))))
  #_(testing "ok"
    (let [result (-> (sut/->sync-factory factory/well-config)
                     (factory/supply :empty-bucket
                                     factory/free)
                     (factory/cook))]
      (is (m/ko? result))
      (is (not (m/ok? result)))
      (is (= :shortage (-> result :ex ex-data :failure-type))))))

#?(:cljs (run-tests))