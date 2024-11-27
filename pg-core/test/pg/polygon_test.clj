(ns pg.polygon-test
  (:import
   (org.pg.type Polygon)
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.test :refer [is deftest testing]]))


(deftest test-encode-bin
  (testing "from object"
    (let [bb (pg/encode-bin (t/polygon [[1 2] [3 4]]) oid/polygon)]
      (is (= [0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from vector"
    (let [bb (pg/encode-bin [[1 2] {:x 3 :y 4}] oid/polygon)]
      (is (= [0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from text"
    (let [bb (pg/encode-bin "((1, 2),(3,4))" oid/polygon)]
      (is (= [0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec))))))


(deftest test-decode-bin
  (let [bb (->bb [0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0])
        p (pg/decode-bin bb oid/polygon)]
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] p))))


(deftest test-decode-txt
  (let [text "  ( (   1.0  , 2.0  )  , (3.0000, 4.0000 ))  "
        p (pg/decode-txt text oid/polygon)]
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] p))))


(deftest test-encode-txt
  (testing "from object"
    (let [p (pg/encode-txt (t/polygon [[1 2] {:x 3 :y 4}]) oid/polygon)]
      (is (= "((1.0,2.0),(3.0,4.0))" p))))

  (testing "from vector"
    (let [p (pg/encode-txt [[1 2] {:x 3 :y 4}] oid/polygon)]
      (is (= "((1.0,2.0),(3.0,4.0))" p))))

  (testing "from text"
    (let [p (pg/encode-txt " (1, 2) , (-3.003, 5.423)" oid/polygon)]
      (is (= "((1.0,2.0),(-3.003,5.423))" p)))))
