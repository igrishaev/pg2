(ns pg.bit-test
  (:import
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.processors :as p]
   [pg.oid :as oid]
   [pg.core :as pg]
   [clojure.test :refer [is deftest]]))


(deftest test-bit-encode-bin
  (let [bb (pg/encode-bin "000100000001000" oid/bit)]
    (is (= [0 0 0 15 16 16]
           (-> bb .array vec))))
  (let [bb (pg/encode-bin "1" oid/bit)]
    (is (= [0 0 0 1 -128]
           (-> bb .array vec))))
  (let [bb (pg/encode-bin (byte-array [1]) oid/bit)]
    (is (= [0 0 0 8 1]
           (-> bb .array vec)))))


(deftest test-bit-decode-bin
  (let [bb (->bb [0 0 0 3 4])]
    (is (= "000"
           (pg/decode-bin bb oid/bit))))
  (let [bb (->bb [0 0 0 14 4 4])]
    (is (= "00000100000001"
           (pg/decode-bin bb oid/bit))))
  (let [bb (->bb [0 0 0 1 -128])]
    (is (= "1"
           (pg/decode-bin bb oid/bit)))))


(deftest test-bit-encode-txt
  (let [res (pg/encode-txt "00000100" oid/bit)]
    (is (= "00000100"
           res)))
  (let [res (pg/encode-txt "1" oid/bit)]
    (is (= "1" res)))
  (let [res (pg/encode-txt (byte-array [1]) oid/bit)]
    (is (= "00000001"
           res))))


(deftest test-bit-decode-txt
  (is (= "0011"
         (pg/decode-txt "0011" oid/bit))))
