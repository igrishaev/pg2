(ns pg.encode-txt-test
  (:import
   org.pg.PGError
   java.math.BigDecimal
   java.math.BigInteger
   java.time.Instant
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetDateTime
   java.time.OffsetTime
   java.time.ZonedDateTime
   java.util.Date)
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [pg.client :as pg]
   [pg.oid :as oid]))


(deftest test-bytea
  (let [res (pg/encode-txt (.getBytes "hello" "UTF-8"))]
    (is (= "\\x68656c6c6f" res))))


(deftest test-encode-basic

  (is (= "1" (pg/encode-txt 1)))

  (is (= "1" (pg/encode-txt (int 1))))

  (is (= "1" (pg/encode-txt (short 1))))

  (is (= "1.1" (pg/encode-txt 1.1)))

  (is (= "1.1" (pg/encode-txt (float 1.1))))

  (is (= "1.0000000000001" (pg/encode-txt 1.0000000000001)))

  (is (= "t" (pg/encode-txt true)))

  (is (= "f" (pg/encode-txt false)))

  (try
    (pg/encode-txt nil)
    (is false)
    (catch PGError e
      (is (= "cannot text-encode a null value"
             (ex-message e)))))

  (let [uuid (random-uuid)]
    (is (= (str uuid) (pg/encode-txt uuid))))

  (is (= "foo/bar" (pg/encode-txt 'foo/bar)))

  (is (= "1.0E+54" (pg/encode-txt (bigdec 999999999999999999999999999999999999999999999999999999.999999))))

  (is (= "?" (pg/encode-txt \?)))

  (let [res (pg/encode-txt (new BigDecimal 999.999))]
    (is (str/starts-with? res "999.999")))

  (let [res (pg/encode-txt (new BigInteger "999"))]
    (is (= "999" res)))

  (let [res (pg/encode-txt (bigint 999.888))]
    (is (= "999" res))))


(deftest test-oid-name

  (let [val 42
        res (pg/encode-txt val oid/oid)]
    (is (= "42" res)))

  (let [val "hello"
        res (pg/encode-txt val oid/name)]
    (is (= "hello" res))))


(deftest test-datetime

  (testing "OffsetDateTime default"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (pg/encode-txt val)]
      (is (= "2023-07-24 22:00:00.123456+00" res))))

  (testing "OffsetDateTime timestamptz"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (pg/encode-txt val oid/timestamptz)]
      (is (= "2023-07-24 22:00:00.123456+00" res))))

  (testing "OffsetDateTime timestamp"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (pg/encode-txt val oid/timestamp)]
      (is (= "2023-07-24 22:00:00.123456" res))))

  (testing "OffsetDateTime date"
    (let [val (OffsetDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (pg/encode-txt val oid/date)]
      (is (= "2023-07-25" res))))

  (testing "LocalDateTime default"
    (let [val (LocalDateTime/parse "2023-07-25T01:00:00.123456")
          res (pg/encode-txt val)]
      (is (= "2023-07-25 01:00:00.123456" res))))

  (testing "LocalDateTime timestamp"
    (let [val (LocalDateTime/parse "2023-07-25T01:00:00.123456")
          res (pg/encode-txt val oid/timestamp)]
      (is (= "2023-07-25 01:00:00.123456" res))))

  (testing "LocalDateTime timestamptz"
    (let [val (LocalDateTime/parse "2023-07-25T01:00:00.123456")
          res (pg/encode-txt val oid/timestamptz)]
      (is (= "2023-07-25 01:00:00.123456+00" res))))

  (testing "LocalDateTime date"
    (let [val (LocalDateTime/parse "2023-07-25T01:00:00.123456")
          res (pg/encode-txt val oid/date)]
      (is (= "2023-07-25" res))))

  (testing "ZonedDateTime default"
    (let [val (ZonedDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (pg/encode-txt val)]
      (is (= "2023-07-24 22:00:00.123456+00" res))))

  (testing "ZonedDateTime timestsamptz"
    (let [val (ZonedDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (pg/encode-txt val oid/timestamptz)]
      (is (= "2023-07-24 22:00:00.123456+00" res))))

  (testing "ZonedDateTime timestsamp"
    (let [val (ZonedDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (pg/encode-txt val oid/timestamp)]
      (is (= "2023-07-24 22:00:00.123456" res))))

  (testing "ZonedDateTime date"
    (let [val (ZonedDateTime/parse "2023-07-25T01:00:00.123456+03")
          res (pg/encode-txt val oid/date)]
      (is (= "2023-07-25" res))))

  (testing "LocalTime default"
    (let [val (LocalTime/parse "01:00:00.123456")
          res (pg/encode-txt val)]
      (is (= "01:00:00.123456" res))))

  (testing "LocalTime time"
    (let [val (LocalTime/parse "01:00:00.123456")
          res (pg/encode-txt val oid/time)]
      (is (= "01:00:00.123456" res))))

  (testing "LocalTime timetz"
    (let [val (LocalTime/parse "01:00:00.123456")
          res (pg/encode-txt val oid/timetz)]
      (is (= "01:00:00.123456+00" res))))

  (testing "OffsetTime default"
    (let [val (OffsetTime/parse "01:00:00.123456+03:00")
          res (pg/encode-txt val)]
      (is (= "01:00:00.123456+03" res))))

  (testing "OffsetTime timetz"
    (let [val (OffsetTime/parse "01:00:00.123456+03:00")
          res (pg/encode-txt val oid/timetz)]
      (is (= "01:00:00.123456+03" res))))

  (testing "OffsetTime time"
    (let [val (OffsetTime/parse "01:00:00.123456+03:00")
          res (pg/encode-txt val oid/time)]
      (is (= "01:00:00.123456" res))))

  (testing "LocalDate default"
    (let [val (LocalDate/parse "2022-01-01")
          res (pg/encode-txt val)]
      (is (= "2022-01-01" res))))

  (testing "LocalDate date"
    (let [val (LocalDate/parse "2022-01-01")
          res (pg/encode-txt val oid/date)]
      (is (= "2022-01-01" res))))

  (testing "LocalDate timestamp"
    (let [val (LocalDate/parse "2022-01-01")
          res (pg/encode-txt val oid/timestamp)]
      (is (= "2022-01-01 00:00:00.000000" res))))

  (testing "LocalDate timestamptz"
    (let [val (LocalDate/parse "2022-01-01")
          res (pg/encode-txt val oid/timestamptz)]
      (is (= "2022-01-01 00:00:00.000000+00" res))))

  (testing "Instant default"
    (let [val (Instant/parse "2023-07-25T01:00:00.123456Z")
          res (pg/encode-txt val)]
      (is (= "2023-07-25 01:00:00.123456+00" res))))

  (testing "Instant timestamptz"
    (let [val (Instant/parse "2023-07-25T01:00:00.123456Z")
          res (pg/encode-txt val oid/timestamptz)]
      (is (= "2023-07-25 01:00:00.123456+00" res))))

  (testing "Instant timestamp"
    (let [val (Instant/parse "2023-07-25T01:00:00.123456Z")
          res (pg/encode-txt val oid/timestamp)]
      (is (= "2023-07-25 01:00:00.123456" res))))

  (testing "Instant date"
    (let [val (Instant/parse "2023-07-25T01:00:00.123456Z")
          res (pg/encode-txt val oid/date)]
      (is (= "2023-07-25" res))))

  (testing "Date default"
    (let [val (-> "2023-07-25T01:00:00.123456Z"
                  (Instant/parse)
                  (.toEpochMilli)
                  (Date.))
          res (pg/encode-txt val)]
      (is (= "2023-07-25 01:00:00.123000+00" res))))

  (testing "Date timestamptz"
    (let [val (-> "2023-07-25T01:00:00.123456Z"
                  (Instant/parse)
                  (.toEpochMilli)
                  (Date.))
          res (pg/encode-txt val oid/timestamptz)]
      (is (= "2023-07-25 01:00:00.123000+00" res))))

  (testing "Date timestamp"
    (let [val (-> "2023-07-25T01:00:00.123456Z"
                  (Instant/parse)
                  (.toEpochMilli)
                  (Date.))
          res (pg/encode-txt val oid/timestamp)]
      (is (= "2023-07-25 01:00:00.123000" res))))

  (testing "Date date"
    (let [val (-> "2023-07-25T01:00:00.123456Z"
                  (Instant/parse)
                  (.toEpochMilli)
                  (Date.))
          res (pg/encode-txt val oid/date)]
      (is (= "2023-07-25" res)))))
