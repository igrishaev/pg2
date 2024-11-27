(ns pg.circle-test
  (:import
   (org.pg.type Circle)
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.test :refer [is deftest testing]]))


(deftest test-encode-bin
  (testing "from object"
    (let [bb (pg/encode-bin (t/circle 1 2 3) oid/circle)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from vector"
    (let [bb (pg/encode-bin [1 2 3] oid/circle)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from text"
    (let [bb (pg/encode-bin " < ( 1.0, 2.0 ) , 3.0 > " oid/circle)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from map"
    (let [bb (pg/encode-bin {:x 1 :y 2 :r 3} oid/circle)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0]
             (-> bb .array vec))))))


(deftest test-decode-bin
  (let [bb (->bb [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0])
        c (pg/decode-bin bb oid/circle)]
    (is (= {:r 3.0, :y 2.0, :x 1.0} c))))


(deftest test-decode-txt
  (let [text " < ( 1.0  ,  2.0 ) , 3.0 > "
        c (pg/decode-txt text oid/circle)]
    (is (= {:r 3.0, :y 2.0, :x 1.0} c))))


(deftest test-encode-txt
  (testing "from object"
    (let [c (pg/encode-txt (t/circle 1 2 3) oid/circle)]
      (is (= "<(1.0,2.0),3.0>" c))))

  (testing "from vector"
    (let [c (pg/encode-txt [1 2 3] oid/circle)]
      (is (= "<(1.0,2.0),3.0>" c))))

  (testing "from text"
    (let [c (pg/encode-txt " < ( 1.0, 2.0 ) , 3.0 > " oid/circle)]
      (is (= "<(1.0,2.0),3.0>" c))))

  (testing "from map"
    (let [c (pg/encode-txt {:x 1 :y 2 :r 3} oid/circle)]
      (is (= "<(1.0,2.0),3.0>" c)))))
