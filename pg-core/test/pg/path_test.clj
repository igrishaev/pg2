(ns pg.path-test
  (:import
   (org.pg.type Path)
   org.pg.error.PGError)
  (:require
   [clojure.pprint :as pprint]
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.string :as str]
   [clojure.test :refer [is deftest testing]]))


(deftest test-encode-bin
  (testing "from object"
    (testing "closed"
      (let [bb (pg/encode-bin (t/path [[1 2] {:x 3 :y 4}]) oid/path)]
        (is (= [1 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
               (-> bb .array vec)))))
    (testing "open"
      (let [bb (pg/encode-bin (t/path [[1 2] {:x 3 :y 4}] false) oid/path)]
        (is (= [0 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
               (-> bb .array vec))))))

  (testing "from vector"
    (let [bb (pg/encode-bin [[1 2] {:x 3 :y 4}] oid/path)]
      (is (= [1 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec)))))

  (testing "from text"
    (testing "open"
      (let [bb (pg/encode-bin "((1, 2),(3,4))" oid/path)]
        (is (= [1 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
               (-> bb .array vec)))))
    (testing "closed"
      (let [bb (pg/encode-bin "[(1, 2),(3,4)]" oid/path)]
        (is (= [0 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
               (-> bb .array vec))))))

  (testing "from map"
    (let [bb (pg/encode-bin {:points [[1 2] {:x 3 :y 4}]} oid/path)]
      (is (= [1 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec))))
    (let [bb (pg/encode-bin {:closed? false
                             :points [[1 2] {:x 3 :y 4}]} oid/path)]
      (is (= [0 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0]
             (-> bb .array vec))))))


(deftest test-decode-bin

  (testing "closed"
    (let [bb (->bb [1 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0])
          p (pg/decode-bin bb oid/path)]
      (is (= {:points [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}],
              :closed? true}
             p))))

  (testing "open"
    (let [bb (->bb [0 0 0 0 2 63 -16 0 0 0 0 0 0 64 0 0 0 0 0 0 0 64 8 0 0 0 0 0 0 64 16 0 0 0 0 0 0])
          p (pg/decode-bin bb oid/path)]
      (is (= {:points [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}],
              :closed? false}
             p)))))


(deftest test-decode-txt

  (testing "closed"
    (let [text "  ( (   1.0  , 2.0  ), (3.0 , 4)  ) "
          p (pg/decode-txt text oid/path)]
      (is (= {:points [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}],
              :closed? true}
             p))))

  (testing "open"
    (let [text "  [ (   1.0  , 2.0  ), (3.0 , 4)  ] "
          p (pg/decode-txt text oid/path)]
      (is (= {:points [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}],
              :closed? false}
             p)))))


(deftest test-encode-txt
  (testing "from object"
    (let [p (pg/encode-txt (t/path [[1 2] {:x 3 :y 4}]) oid/path)]
      (is (= "((1.0,2.0),(3.0,4.0))" p)))
    (let [p (pg/encode-txt (t/path [[1 2] {:x 3 :y 4}] false) oid/path)]
      (is (= "[(1.0,2.0),(3.0,4.0)]" p))))

  (testing "from vector"
    (let [p (pg/encode-txt [[1 2] {:x 3 :y 4}] oid/path)]
      (is (= "((1.0,2.0),(3.0,4.0))" p))))

  (testing "from text"
    (let [p (pg/encode-txt " ((1, 2),(3,4)) " oid/path)]
      (is (= "((1.0,2.0),(3.0,4.0))" p)))
    (let [p (pg/encode-txt "[ (1, 2),(3,4)   ] " oid/path)]
      (is (= "[(1.0,2.0),(3.0,4.0)]" p))))

  (testing "from map"
    (let [p (pg/encode-txt {:closed? true
                            :points [[1 2] {:x 3 :y 4}]} oid/path)]
      (is (= "((1.0,2.0),(3.0,4.0))" p)))
    (let [p (pg/encode-txt {:closed? false
                            :points [[1 2] {:x 3 :y 4}]} oid/path)]
      (is (= "[(1.0,2.0),(3.0,4.0)]" p)))
    (let [p (pg/encode-txt {:closed? nil
                            :points (repeat 3 [1 2])} oid/path)]
      (is (= "((1.0,2.0),(1.0,2.0),(1.0,2.0))" p)))))
