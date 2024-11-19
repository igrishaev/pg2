(ns pg.point-test
  (:import
   (org.pg.type Point)
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.test :refer [is deftest testing]]))


(deftest test-props
  (let [p (t/point 1 2)]
    (is (instance? Point p))
    (is (= "(1.0,2.0)" (str p)))
    (is (= "<Point (1.0,2.0)>" (pr-str p)))
    (is (= {:y 2.0, :x 1.0} @p))

    (is (= 1.0 (:x p)))
    (is (= 2.0 (:y p)))

    (is (= 1.0 (nth p 0)))
    (is (= 2.0 (nth p 1)))

    (is (= 2.0 (get p :y)))

    (is (nil? (get p "abc")))

    (is (= 2 (count p)))
    (is (= [1.0 2.0] (vec p)))

    (is (= ::miss (nth p 99 ::miss)))

    (try
      (nth p 2)
      (is false)
      (catch IndexOutOfBoundsException e
        (is true)))))


(deftest test-encode-bin
  (testing "from object"
    (let [bb (pg/encode-bin (t/point 1 2) oid/point)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from vector"
    (let [bb (pg/encode-bin [1 2] oid/point)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from text"
    (let [bb (pg/encode-bin "(1, 2)" oid/point)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from map"
    (let [bb (pg/encode-bin {:x 1 :y 2} oid/point)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0]
             (-> bb .array vec))))))


(deftest test-decode-bin
  (let [bb (->bb [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0])
        p (pg/decode-bin bb oid/point)]
    (is (= {:y 2.0, :x 1.0} @p))))


(deftest test-decode-txt
  (let [text "   (   1.0  , 2.0  )   "
        p (pg/decode-txt text oid/point)]
    (is (= {:y 2.0, :x 1.0} @p))))


(deftest test-encode-txt
  (testing "from object"
    (let [p (pg/encode-txt (t/point 1 2) oid/point)]
      (is (= "(1.0,2.0)" p))))

  (testing "from vector"
    (let [p (pg/encode-txt [1 2] oid/point)]
      (is (= "(1.0,2.0)" p))))

  (testing "from text"
    (let [p (pg/encode-txt " (1, 2) " oid/point)]
      (is (= "(1.0,2.0)" p))))

  (testing "from map"
    (let [p (pg/encode-txt {:x 1 :y 2} oid/point)]
      (is (= "(1.0,2.0)" p)))))
