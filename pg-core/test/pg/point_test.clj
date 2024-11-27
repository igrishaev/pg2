(ns pg.point-test
  (:import
   (org.pg.type Point)
   org.pg.error.PGError)
  (:require
   [clojure.pprint :as pprint]
   [jsonista.core :as js]
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.string :as str]
   [clojure.test :refer [is deftest testing]]))

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
    (is (= {:y 2.0, :x 1.0} p))))


(deftest test-decode-txt
  (let [text "   (   1.0  , 2.0  )   "
        p (pg/decode-txt text oid/point)]
    (is (= {:y 2.0, :x 1.0} p))))


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
