(ns pg.line-test
  (:import
   (org.pg.type Line)
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.test :refer [is deftest testing]]))

(deftest test-props
  (let [l (t/line 1 2 3)]
    (is (instance? Line l))

    (is (= "{1.0,2.0,3.0}" (str l)))
    (is (= "<Line {1.0,2.0,3.0}>" (pr-str l)))
    (is (= {:a 1.0 :b 2.0 :c 3.0} @l))

    (is (= 1.0 (:a l)))
    (is (= 2.0 (:b l)))
    (is (= 3.0 (:c l)))

    (is (= 1.0 (nth l 0)))
    (is (= 2.0 (nth l 1)))
    (is (= 3.0 (nth l 2)))
    (is (= ::miss (nth l 99 ::miss)))

    (is (= :test (get l :dunno :test)))
    (is (nil? (get l :dunno)))

    (is (= 3 (count l)))
    (is (= [1.0 2.0 3.0] (vec l)))

    (try
      (nth l 3)
      (is false)
      (catch IndexOutOfBoundsException e
        (is true)))))


(deftest test-encode-bin
  (testing "from object"
    (let [bb (pg/encode-bin (t/line 1 2 3) oid/line)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from vector"
    (let [bb (pg/encode-bin [1 2 3] oid/line)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from text"
    (let [bb (pg/encode-bin " { 1.0, 2.0 , 3.0 } " oid/line)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from map"
    (let [bb (pg/encode-bin {:a 1 :b 2 :c 3} oid/line)]
      (is (= [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0]
             (-> bb .array vec))))))


(deftest test-decode-bin
  (let [bb (->bb [63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0])
        p (pg/decode-bin bb oid/line)]
    (is (= {:c 3.0, :b 2.0, :a 1.0} @p))))


(deftest test-decode-txt
  (let [text " { 1.0  ,  2.0  , 3.0 } "
        p (pg/decode-txt text oid/line)]
    (is (= {:c 3.0, :b 2.0, :a 1.0} @p))))


(deftest test-encode-txt
  (testing "from object"
    (let [l (pg/encode-txt (t/line 1 2 3) oid/line)]
      (is (= "{1.0,2.0,3.0}" l))))

  (testing "from vector"
    (let [l (pg/encode-txt [1 2 3] oid/line)]
      (is (= "{1.0,2.0,3.0}" l))))

  (testing "from text"
    (let [l (pg/encode-txt " { 1.0, 2.0 , 3.0 } " oid/line)]
      (is (= "{1.0,2.0,3.0}" l))))

  (testing "from map"
    (let [l (pg/encode-txt {:a 1 :b 2 :c 3} oid/line)]
      (is (= "{1.0,2.0,3.0}" l)))))
