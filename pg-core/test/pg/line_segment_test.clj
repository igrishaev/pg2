(ns pg.line-segment-test
  (:import
   (org.pg.type LineSegment)
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.test :refer [is deftest testing]]))


(deftest test-encode-bin
  (testing "from object"
    (let [bb (pg/encode-bin (t/line-segment 1 2 3 4) oid/lseg)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from vector"
    (let [bb (pg/encode-bin [[1 2] [3 4]] oid/lseg)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from text"
    (let [bb (pg/encode-bin "[ ( 1.0, 2.0 ) , (3.0 , 4.0 )] " oid/lseg)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from map"
    (let [bb (pg/encode-bin {:x1 1 :y1 2 :x2 3 :y2 4} oid/lseg)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec))))))


(deftest test-decode-bin
  (let [bb (->bb [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0])
        x (pg/decode-bin bb oid/lseg)]
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] x))))


(deftest test-decode-txt
  (let [text " ( 1.0, 2.0 ) , (3.0 , 4.0 ) "
        x (pg/decode-txt text oid/lseg)]
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] x))))


(deftest test-encode-txt
  (testing "from object"
    (let [x (pg/encode-txt (t/line-segment [[1 2] [3 4]]) oid/lseg)]
      (is (= "[(1.0,2.0),(3.0,4.0)]" x))))

  (testing "from vector"
    (let [x (pg/encode-txt [[1 2] [3 4]] oid/lseg)]
      (is (= "[(1.0,2.0),(3.0,4.0)]" x))))

  (testing "from text"
    (let [x (pg/encode-txt " [ ( 1.0, 2.0 ) ,( 3.0, 4) ] " oid/lseg)]
      (is (= "[(1.0,2.0),(3.0,4.0)]" x))))

  (testing "from map"
    (let [x (pg/encode-txt {:x1 1 :y1 2 :x2 3 :y2 4} oid/lseg)]
      (is (= "[(1.0,2.0),(3.0,4.0)]" x)))))
