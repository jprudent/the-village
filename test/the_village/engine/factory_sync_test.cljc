(ns the-village.engine.factory-sync-test
  (:require
    #?(:clj  [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is run-tests]])))


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
