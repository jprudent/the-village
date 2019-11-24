(ns the-village.engine.villager-test
  (:require #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is run-tests]])
            [the-village.engine.villager :as sut]
            [the-village.engine.villager :as villager]))

(defn tick! [time] (swap! time inc))

(deftest villager-test
  (testing "last update and vitals"
    (let [time       (atom 0)
          conf       {:id-provider   (constantly 0)
                      :time-provider #(deref time)
                      :game-speed    #(* 0.01 %)}
          villager   (sut/spawn conf)

          _          (tick! time)
          villager'  (sut/update-villager villager conf)
          _          (is (= @time (:last-update villager')))
          _          (is (< (:hydration-level villager') 1))

          _          (tick! time)
          villager'' (sut/update-villager villager' conf)
          _          (is (> (:last-update villager'') (:last-update villager')))
          _          (is (< (:hydration-level villager'') (:hydration-level villager')))]))

  (testing "drink will but no journey possible"
    (let [time       (atom 0)
          conf       {:id-provider   (constantly 0)
                      :time-provider #(deref time)
                      :game-speed    #(* 0.01 %)
                      :path-finder   (constantly nil)}
          villager   (assoc (sut/spawn conf)
                       :hydration-level 0.1)

          _          (tick! time)
          villager'  (sut/update-villager villager conf)
          _          (is (= [:villager.will/drink] (:wills villager')))
          _          (is (not (villager/on-journey? villager')))
          _          (prn villager')

          _          (tick! time)
          villager'' (sut/update-villager villager' conf)
          _          (is (= [:villager.will/drink] (:wills villager''))
                         "The will is not added twice")]))

  (testing "drink will with a journey possible"
    (let [time      (atom 0)
          conf      {:id-provider   (constantly 0)
                     :time-provider #(deref time)
                     :game-speed    #(* 0.01 %)
                     :path-finder   (constantly [[1 2]])}
          villager  (assoc (sut/spawn conf)
                      :wills '(:villager.will/drink))

          _         (tick! time)
          villager' (sut/update-villager villager conf)
          _         (is (= [:villager.will/drink] (:wills villager')))
          _         (is (villager/on-journey? villager'))])))
