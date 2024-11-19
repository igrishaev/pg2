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


(deftest test-props
  (let [c (t/circle 1 2 3)]
    (is (t/circle? c))

    (is (= "<(1.0,2.0),3.0>" (str c)))
    (is (= "<Circle <(1.0,2.0),3.0>>" (pr-str c)))
    (is (= {:x 1.0 :y 2.0 :r 3.0} @c))

    (is (= 1.0 (:x c)))
    (is (= 2.0 (:y c)))
    (is (= 3.0 (:r c)))
    (is (= ::miss (nth c 99 ::miss)))

    (is (= 1.0 (nth c 0)))
    (is (= 2.0 (nth c 1)))
    (is (= 3.0 (nth c 2)))

    (is (= :test (get c :dunno :test)))
    (is (nil? (get c :dunno)))

    (is (= 3 (count c)))
    (is (= [1.0 2.0 3.0] (vec c)))

    (try
      (nth c 3)
      (is false)
      (catch IndexOutOfBoundsException e
        (is true)))))


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
    (is (= {:r 3.0, :y 2.0, :x 1.0} @c))))


(deftest test-decode-txt
  (let [text " < ( 1.0  ,  2.0 ) , 3.0 > "
        c (pg/decode-txt text oid/circle)]
    (is (= {:r 3.0, :y 2.0, :x 1.0} @c))))


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
