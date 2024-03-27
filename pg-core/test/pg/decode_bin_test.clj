(ns pg.decode-bin-test
  (:import
   java.math.BigDecimal
   java.math.BigDecimal
   java.nio.ByteBuffer
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetDateTime
   java.time.OffsetTime
   java.util.UUID)
  (:require
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as j]
   [pg.bb :refer [->bb]]
   [pg.core :as pg]
   [pg.oid :as oid]))


(defn reverse-string [s]
  (apply str (reverse s)))


(def custom-mapper
  (j/object-mapper
   {:encode-key-fn (comp reverse-string name)
    :decode-key-fn (comp keyword reverse-string)}))


(deftest test-bytea
  (let [res (pg/decode-bin (->bb (byte-array [104 101 108 108 111])) oid/bytea)]
    (is (= [104 101 108 108 111] (-> res vec)))))


(deftest test-uuid

  (let [buf
        (->bb [-69 -39 -49 124 78 1 78 115 -103 -87 -115 94 88 11 -64 20])

        uuid
        (pg/decode-bin buf oid/uuid)]

    (is (= #uuid "bbd9cf7c-4e01-4e73-99a9-8d5e580bc014" uuid))))


(deftest test-datetime

  (testing "timestamptz"

    (let [buf ;; 2022-01-01 12:01:59.123456789+03
          (->bb [0 2 119 -128 79 11 -14 1])

          res
          (pg/decode-bin buf oid/timestamptz)]

      (is (= "2022-01-01T09:01:59.123457Z" (str res)))
      (is (instance? OffsetDateTime res))))

  (testing "timestamp"

    (let [buf ;; 2022-01-01 12:01:59.123456789
          (->bb [0 2 119 -126 -46 -58 -34 1])

          res
          (pg/decode-bin buf oid/timestamp)]

      (is (= "2022-01-01T12:01:59.123457" (str res)))
      (is (instance? LocalDateTime res))))

  (testing "date"

    (let [buf ;; 2022-01-01 12:01:59.123456789+03
          (->bb [0 0 31 100])

          res
          (pg/decode-bin buf oid/date)]

      (is (= "2022-01-01" (str res)))
      (is (instance? LocalDate res))))

  (testing "timetz"

    (let [buf ;; 12:01:59.123456789+03
          (->bb [0 0 0 10 22 5 94 1 -1 -1 -43 -48])

          res
          (pg/decode-bin buf oid/timetz)]

      (is (= "12:01:59.123457+03:00" (str res)))
      (is (instance? OffsetTime res))))

  (testing "time"

    (let [buf ;; 12:01:59.123456789+03
          (->bb [0 0 0 10 22 5 94 1])

          res
          (pg/decode-bin buf oid/time)]

      (is (= "12:01:59.123457" (str res)))
      (is (instance? LocalTime res)))))


(deftest test-oid-bytea
  (let [buf
        (->bb [1 2 3 4])]
    (is (= [1 2 3 4]
           (vec (pg/decode-bin buf oid/bytea))))))


(deftest test-oid-name

  (let [buf
        (->bb [0 0 0 1])

        res
        (pg/decode-bin buf oid/oid)]

    (is (= 1 res))
    (is (instance? Integer res)))

  (let [buf
        (->bb [43 43 43 43])

        res
        (pg/decode-bin buf oid/name)]

    (is (= "++++" res))
    (is (instance? String res))))


(deftest test-numeric

  (let [buf
        (->bb [0 2 0 0 0 0 0 3 0 123 17 -48])

        res
        (pg/decode-bin buf oid/numeric)]

    (is (instance? BigDecimal res))
    (is (= 123.456M res)))

  (let [buf
        (->bb [0 2 0 0 64 0 0 3 0 123 17 -48])

        res
        (pg/decode-bin buf oid/numeric)]

    (is (instance? BigDecimal res))
    (is (= -123.456M res))))


(deftest test-json-custom-mapper
  (let [string
        (j/write-value-as-string {:foo 42})

        bb
        (ByteBuffer/wrap (.getBytes string))

        data
        (pg/decode-bin bb
                       oid/json
                       {:object-mapper custom-mapper})]

    (is (= {:oof 42} data))))


(def BUF-ARRAY-2X3-INT4
  (->bb
   [0,  0,  0,  2,  ;; dims
    0,  0,  0,  1,  ;; nulls true
    0,  0,  0,  23, ;; oid
    0,  0,  0,  2,  ;; dim1 = 2
    0,  0,  0,  1,  ;; ?
    0,  0,  0,  3,  ;; dim2 = 3
    0,  0,  0,  1,  ;; ?
    0,  0,  0,  4,  ;; len
    0,  0,  0,  1,  ;; 1
    0,  0,  0,  4,  ;; len
    0,  0,  0,  2,  ;; 2
    0,  0,  0,  4,  ;; len
    0,  0,  0,  3,  ;; 3
    0,  0,  0,  4,  ;; len
    0,  0,  0,  4,  ;; 4
    -1, -1, -1, -1, ;; null
    0,  0,  0,  4,  ;; len = 4
    0,  0,  0,  6   ;; 6
    ]))


(deftest test-arrays
  (let [res
        (pg/decode-bin BUF-ARRAY-2X3-INT4 oid/_int4)]
    (is (= [[1 2 3] [4 nil 6]]
           res))))
