(ns pg.box-test
  (:import
   (org.pg.type Box)
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.test :refer [is deftest testing]]))


(deftest test-props

  (is (= (t/box [1 2] [3 4])
         (t/box [3 4] [1 2])))

  (is (= (t/box [1 2] [3 4])
         (t/box [1 2] [3 4])))

  (is (not= (t/box [1 2] [3 4])
            (t/box [3 4] [3 4])))

  (let [x (t/box 1 2 3 4)]

    (is (t/box? x))

    (is (= "(1.0,2.0),(3.0,4.0)" (str x)))
    (is (= "<Box (1.0,2.0),(3.0,4.0)>" (pr-str x)))
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] @x))

    (is (= 1.0 (:x1 x)))
    (is (= 2.0 (:y1 x)))
    (is (= 3.0 (:x2 x)))
    (is (= 4.0 (:y2 x)))

    (is (= ::miss (nth x 99 ::miss)))

    (is (= {:y 2.0, :x 1.0} @(nth x 0)))
    (is (= {:y 4.0, :x 3.0} @(nth x 1)))

    (is (= :test (get x :dunno :test)))
    (is (nil? (get x :dunno)))

    (is (= 2 (count x)))
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] (mapv deref x)))

    (try
      (nth x 3)
      (is false)
      (catch IndexOutOfBoundsException e
        (is true))))

  (let [x (t/box "(1,2),(3,4)")]
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] @x))
    (is (= "(1.0,2.0),(3.0,4.0)" (str x)))))


(deftest test-encode-bin
  (testing "from object"
    (let [bb (pg/encode-bin (t/box 1 2 3 4) oid/box)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from vector"
    (let [bb (pg/encode-bin [[1 2] [3 4]] oid/box)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from text"
    (let [bb (pg/encode-bin " ( 1.0, 2.0 ) , (3.0 , 4.0 ) " oid/box)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from map"
    (let [bb (pg/encode-bin {:x1 1 :y1 2 :x2 3 :y2 4} oid/box)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec))))))


(deftest test-decode-bin
  (let [bb (->bb [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0])
        x (pg/decode-bin bb oid/box)]
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] @x))))

(deftest test-decode-txt
  (let [text " ( 1.0, 2.0 ) , (3.0 , 4.0 ) "
        x (pg/decode-txt text oid/box)]
    (is (= [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}] @x))))


(deftest test-encode-txt
  (testing "from object"
    (let [x (pg/encode-txt (t/box [[1 2] [3 4]]) oid/box)]
      (is (= "(1.0,2.0),(3.0,4.0)" x))))

  (testing "from vector"
    (let [x (pg/encode-txt [[1 2] [3 4]] oid/box)]
      (is (= "(1.0,2.0),(3.0,4.0)" x))))

  (testing "from text"
    (let [x (pg/encode-txt "  ( 1.0, 2.0 ) ,( 3.0, 4)  " oid/box)]
      (is (= "(1.0,2.0),(3.0,4.0)" x))))

  (testing "from map"
    (let [x (pg/encode-txt {:x1 1 :y1 2 :x2 3 :y2 4} oid/box)]
      (is (= "(1.0,2.0),(3.0,4.0)" x)))))
