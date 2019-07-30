(ns the-village.engine.path-finder-test
  (:require #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest is run-tests testing]])
            [the-village.engine.path-finder :as sut]))

(deftest find-path*-test
  (testing "the simplest path finding"
    (is (= '([0 0])
           (sut/find-path
             {}
             [1 1]
             [0 0]
             [0 0]))))

  (testing "a 2 cells grid"
    (is (= '([0 0] [0 1])
           (sut/find-path
             {}
             [1 2]
             [0 0]
             [0 1]))))

  (testing "teleport through upper edge"
    (is (= '([0 0] [0 2])
           (sut/find-path
             {[0 1] :boulder}
             [1 3]
             [0 0]
             [0 2]))))

  (testing "biggest map, only one shortest possibility"
    (is (= '([0 0] [2 0] [2 2])
           (sut/find-path
             {[0 1] :boulder
              [0 2] :pit
              [1 1] :black-hole
              [1 2] :spinach}
             [3 3]
             [0 0]
             [2 2]))))

  (testing "impossible"
    (is (= nil
           (sut/find-path
             {[1 0] :boulder
              [3 0] :pit}
             [4 1]
             [0 0]
             [2 0]))))

  (testing "I want to go in the tar pit, but I cant' climb boulders"
    (is (= '([0 0] [3 0] [2 0])
           (sut/find-path
             {[1 0] :boulder
              [2 0] :tar-pit}
             [4 1]
             [0 0]
             [2 0]))))

  (testing "real test case"
     (is (= '([0 0])
            (sut/find-path
              {'(3 4) :well}
              [10 8]
              [0 0]
              [3 4])))))
