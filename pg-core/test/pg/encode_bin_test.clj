(ns pg.encode-bin-test
  (:import
   java.math.BigDecimal
   java.math.BigInteger
   java.nio.ByteBuffer
   java.time.Instant
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetDateTime
   java.time.OffsetTime
   java.time.ZoneOffset
   java.time.ZonedDateTime
   java.util.Date
   org.pg.error.PGError)
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as j]
   [pg.bb :refer [bb==]]
   [pg.core :as pg]
   [pg.oid :as oid]))


(defn reverse-string [s]
  (apply str (reverse s)))


(def custom-mapper
  (j/object-mapper
   {:encode-key-fn (comp reverse-string name)
    :decode-key-fn (comp keyword reverse-string)}))


(deftest test-bytea
  (let [res (pg/encode-bin (.getBytes "hello" "UTF-8"))]
    (is (= [104 101 108 108 111] (-> res .array vec)))))


(deftest test-null
  (try
    (pg/encode-bin nil)
    (is false)
    (catch PGError e
      (is (= "cannot binary-encode a null value"
             (ex-message e))))))


(deftest test-numbers

  ;; int

  (let [res (pg/encode-bin 1)]
    (is (bb== (byte-array [0 0 0 0 0 0 0 1]) res)))

  (let [res (pg/encode-bin (int 1))]
    (is (bb== (byte-array [0 0 0 1]) res)))

  (let [res (pg/encode-bin (short 1))]
    (is (bb== (byte-array [0 1]) res)))

  ;; byte

  (let [res (pg/encode-bin (byte 1))]
    (is (bb== (byte-array [0 1]) res)))

  (let [res (pg/encode-bin (byte 1) oid/int4)]
    (is (bb== (byte-array [0 0 0 1]) res)))

  (let [res (pg/encode-bin (byte 1) oid/int8)]
    (is (bb== (byte-array [0 0 0 0 0 0 0 1]) res)))

  ;; float

  (let [res (pg/encode-bin (float 1.1) oid/float4)]
    (is (bb== (byte-array [63, -116, -52, -51]) res)))

  (let [res (pg/encode-bin (double 1.1) oid/float8)]
    (is (bb== (byte-array [63, -15, -103, -103, -103, -103, -103, -102]) res))
    (is (= (double 1.1) (pg/decode-bin res oid/float8))))

  ;; int -> float

  (let [res (pg/encode-bin (short 1) oid/float4)]
    (is (bb== (byte-array [63, -128, 0, 0]) res)))

  (let [res (pg/encode-bin (int 1) oid/float4)]
    (is (bb== (byte-array [63, -128, 0, 0]) res)))

  (let [res (pg/encode-bin (long 1) oid/float4)]
    (is (bb== (byte-array [63, -128, 0, 0]) res)))

  (let [res (pg/encode-bin (short 1) oid/float8)]
    (is (bb== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  (let [res (pg/encode-bin (int 1) oid/float8)]
    (is (bb== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  (let [res (pg/encode-bin (long 1) oid/float8)]
    (is (bb== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  (let [res (pg/encode-bin (long 1) oid/float8)]
    (is (bb== (byte-array [63, -16, 0, 0, 0, 0, 0, 0]) res)))

  ;; bigint

  (let [res (pg/encode-bin (bigint 1) oid/numeric)]
    (is (bb== (byte-array [0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0]) res)))

  (let [res (pg/encode-bin (bigint 1) oid/int2)]
    (is (bb== (byte-array [0 1]) res)))

  (let [res (pg/encode-bin (bigint 1) oid/int4)]
    (is (bb== (byte-array [0 0 0 1]) res)))

  (let [res (pg/encode-bin (bigint 1) oid/int8)]
    (is (bb== (byte-array [0 0 0 0 0 0 0 1]) res)))

  ;; biginteger

  (let [res (pg/encode-bin (new BigInteger "1") oid/numeric)]
    (is (bb== (byte-array [0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0]) res)))

  (let [res (pg/encode-bin (new BigInteger "1") oid/int2)]
    (is (bb== (byte-array [0 1]) res)))

  (let [res (pg/encode-bin (new BigInteger "1") oid/int4)]
    (is (bb== (byte-array [0 0 0 1]) res)))

  (let [res (pg/encode-bin (new BigInteger "1") oid/int8)]
    (is (bb== (byte-array [0 0 0 0 0 0 0 1]) res))))


(deftest test-json-custom-mapper
  (let [bb
        (pg/encode-bin {:foo 123}
                       oid/json
                       {:object-mapper custom-mapper})

        string
        (-> bb .rewind .array String.)

        data
        (j/read-value string)]

    (is (= {"oof" 123} data))))


(deftest test-json-encode-bin

  (testing "json coll"
    (let [bb (pg/encode-bin [1 2 3] oid/json)]
      (is (= [91 49 44 50 44 51 93] (-> bb .array vec)))))

  (testing "jsonb coll"
    (let [bb (pg/encode-bin [1 2 3] oid/jsonb)]
      (is (= [1 91 49 44 50 44 51 93] (-> bb .array vec)))))

  (testing "default coll"
    (let [bb (pg/encode-bin [1 2 3])]
      (is (= [1 91 49 44 50 44 51 93]
             (-> bb .array vec)))))

  (testing "json string"
    (let [bb (pg/encode-bin "[1,2,3]" oid/json)]
      (is (= [91 49 44 50 44 51 93] (-> bb .array vec)))))

  (testing "jsonb string"
    (let [bb (pg/encode-bin "[1,2,3]" oid/jsonb)]
      (is (= [1 91 49 44 50 44 51 93] (-> bb .array vec)))))

  (testing "jsonb number"
    (let [bb (pg/encode-bin 1 oid/jsonb)]
      (is (= [1 49] (-> bb .array vec)))))

  (testing "json number"
    (let [bb (pg/encode-bin 1 oid/json)]
      (is (= [49] (-> bb .array vec)))))

  (testing "default wrapper number"
    (let [bb (pg/encode-bin (pg/json-wrap 1))]
      (is (= [1 49] (-> bb .array vec)))))

  (testing "json wrapper number"
    (let [bb (pg/encode-bin (pg/json-wrap 1) oid/json)]
      (is (= [49] (-> bb .array vec))))))


(deftest test-big-decimal

  (doseq [value ["0"
                 "1"
                 "-1"
                 "123.456789"
                 "1234.56789"
                 "12345.6789"
                 "12345678.9"
                 "1.23456789"
                 "0.123456789"
                 "0.000000000000000000000123456789"
                 "123123123123123.000000000000000000000009"
                 "-0.00000000000000000000000100500"
                 "-23523423623423236212463460.00000000000000000000000100500333"
                 "342e10"
                 "-123e-8"]]

    (let [x1 (bigdec value)
          buf (pg/encode-bin x1 oid/numeric)
          x2 (pg/decode-bin buf oid/numeric)]
      (is (= x1 x2))))

  (let [x1 (bigdec "1")
        buf (pg/encode-bin x1 oid/int2)]
    (is (bb== (byte-array [0 1]) buf)))

  (let [x1 (bigdec "1")
        buf (pg/encode-bin x1 oid/int4)]
    (is (bb== (byte-array [0 0 0 1]) buf)))

  (let [x1 (bigdec "1")
        buf (pg/encode-bin x1 oid/int8)]
    (is (bb== (byte-array [0 0 0 0 0 0 0 1]) buf)))

  (let [x1 (bigdec "1.1")
        buf (pg/encode-bin x1 oid/float4)]
    (is (bb== (byte-array [63, -116, -52, -51]) buf)))

  (let [x1 (bigdec "1.1")
        buf (pg/encode-bin x1 oid/float8)
        x2 (pg/decode-bin buf oid/float8)]
    (is (= (str x1) (str x2)))
    (is (bb== (byte-array [63 -15 -103 -103 -103 -103 -103 -102]) buf))))


(deftest test-datetime

  ;; OffsetTime

  (testing "default"
    (let [val1 (OffsetTime/now)
          buf (pg/encode-bin val1)
          val2 (pg/decode-bin buf oid/timetz)]
      (is (= val1 val2))))

  (testing "timetz"
    (let [val1 (OffsetTime/now)
          buf (pg/encode-bin val1 oid/timetz)
          val2 (pg/decode-bin buf oid/timetz)]
      (is (= val1 val2))))

  (testing "time"
    (let [val1 (OffsetTime/parse "12:28:23.336188+03:00")
          buf (pg/encode-bin val1 oid/time)
          val2 (pg/decode-bin buf oid/time)]
      (is (= (LocalTime/parse "12:28:23.336188") val2))))

  ;; LocalTime

  (testing "default"
    (let [val1 (LocalTime/now)
          buf (pg/encode-bin val1)
          val2 (pg/decode-bin buf oid/time)]
      (is (= val1 val2))))

  (testing "time"
    (let [val1 (LocalTime/now)
          buf (pg/encode-bin val1 oid/time)
          val2 (pg/decode-bin buf oid/time)]
      (is (= val1 val2))))

  (testing "timetz"
    (let [val1 (LocalTime/parse "12:41:00.005652")
          buf (pg/encode-bin val1 oid/timetz)
          val2 (pg/decode-bin buf oid/timetz)]
      (is (= (OffsetTime/parse "12:41:00.005652Z") val2))))

  ;; LocalDate

  (testing "default"
    (let [val1 (LocalDate/now)
          buf (pg/encode-bin val1)
          val2 (pg/decode-bin buf oid/date)]
      (is (= val1 val2))))

  (testing "date"
    (let [val1 (LocalDate/now)
          buf (pg/encode-bin val1 oid/date)
          val2 (pg/decode-bin buf oid/date)]
      (is (= val1 val2))))

  (testing "timestamp"
    (let [val1 (LocalDate/parse "2022-01-01")
          buf (pg/encode-bin val1 oid/timestamp)
          val2 (pg/decode-bin buf oid/timestamp)]
      (is (= (LocalDateTime/parse "2022-01-01T00:00") val2))))

  (testing "timestamptz"
    (let [val1 (LocalDate/parse "2022-01-01")
          buf (pg/encode-bin val1 oid/timestamptz)
          val2 (pg/decode-bin buf oid/timestamptz)]
      (is (= (OffsetDateTime/parse "2022-01-01T00:00Z") val2))))

  ;; OffsetDateTime

  (testing "default"
    (let [val1 (OffsetDateTime/now)
          buf (pg/encode-bin val1)
          val2 ^OffsetDateTime (pg/decode-bin buf oid/timestamptz)]
      (is (instance? OffsetDateTime val2))
      (is (= (.atZoneSameInstant val1 ZoneOffset/UTC)
             (.atZoneSameInstant val2 ZoneOffset/UTC)))))

  (testing "timestamptz"
    (let [val1 (OffsetDateTime/now)
          buf (pg/encode-bin val1 oid/timestamptz)
          val2 ^OffsetDateTime (pg/decode-bin buf oid/timestamptz)]
      (is (instance? OffsetDateTime val2))
      (is (= (.atZoneSameInstant val1 ZoneOffset/UTC)
             (.atZoneSameInstant val2 ZoneOffset/UTC)))))

  (testing "timestamp"
    (let [val1 (OffsetDateTime/parse "2023-07-27T12:44:20.611698+03:00")
          buf (pg/encode-bin val1 oid/timestamp)
          val2 (pg/decode-bin buf oid/timestamp)]
      (is (= (LocalDateTime/parse "2023-07-27T09:44:20.611698") val2))))

  (testing "date"
    (let [val1 (OffsetDateTime/parse "2023-07-27T12:44:20.611698+03:00")
          buf (pg/encode-bin val1 oid/date)
          val2 (pg/decode-bin buf oid/date)]
      (is (= (LocalDate/parse "2023-07-27") val2))))

  ;; ZonedDateTime

  (testing "default"
    (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
          buf (pg/encode-bin val1)
          val2 (pg/decode-bin buf oid/timestamptz)]
      (is (= (OffsetDateTime/parse "2023-07-26T22:56:35.028508Z") val2))))

  (testing "timestamptz"
    (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
          buf (pg/encode-bin val1 oid/timestamptz)
          val2 (pg/decode-bin buf oid/timestamptz)]
      (is (= (OffsetDateTime/parse "2023-07-26T22:56:35.028508Z") val2))))

  (testing "timestamp"
    (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
          buf (pg/encode-bin val1 oid/timestamp)
          val2 (pg/decode-bin buf oid/timestamp)]
      (is (= (LocalDateTime/parse "2023-07-26T22:56:35.028508") val2))))

  (testing "date"
    (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
          buf (pg/encode-bin val1 oid/date)
          val2 (pg/decode-bin buf oid/date)]
      (is (= (LocalDate/parse "2023-07-27") val2))))

  ;; Instant

  (testing "default"
    (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
          buf (pg/encode-bin val1)
          val2 (pg/decode-bin buf oid/timestamptz)]
      (is (= (OffsetDateTime/parse "2023-07-27T01:25:55.297834Z") val2))))

  (testing "timestamptz"
    (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
          buf (pg/encode-bin val1 oid/timestamptz)
          val2 (pg/decode-bin buf oid/timestamptz)]
      (is (= (OffsetDateTime/parse "2023-07-27T01:25:55.297834Z") val2))))

  (testing "timestamp"
    (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
          buf (pg/encode-bin val1 oid/timestamp)
          val2 (pg/decode-bin buf oid/timestamp)]
      (is (= (LocalDateTime/parse "2023-07-27T01:25:55.297834") val2))))

  (testing "date"
    (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
          buf (pg/encode-bin val1 oid/date)
          val2 (pg/decode-bin buf oid/date)]
      (is (= (LocalDate/parse "2023-07-27") val2))))

  ;; LocalDateTime

  (testing "default"
    (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
          buf (pg/encode-bin val1)
          val2 (pg/decode-bin buf oid/timestamptz)]
      (is (= (OffsetDateTime/parse "2023-07-27T01:31:21.025913Z") val2))))

  (testing "timestamptz"
    (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
          buf (pg/encode-bin val1 oid/timestamptz)
          val2 (pg/decode-bin buf oid/timestamptz)]
      (is (= (OffsetDateTime/parse "2023-07-27T01:31:21.025913Z") val2))))

  (testing "timestamp"
    (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
          buf (pg/encode-bin val1 oid/timestamp)
          val2 (pg/decode-bin buf oid/timestamp)]
      (is (= (LocalDateTime/parse "2023-07-27T01:31:21.025913") val2))))

  (testing "data"
    (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
          buf (pg/encode-bin val1 oid/date)
          val2 (pg/decode-bin buf oid/date)]
      (is (= (LocalDate/parse "2023-07-27") val2))))

  ;; Date

  (testing "default"
    (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                   (Instant/parse)
                   (.toEpochMilli)
                   (Date.))
          buf (pg/encode-bin val1)
          val2 (pg/decode-bin buf oid/date)]
      (is (= (LocalDate/parse "2023-07-25") val2))))

  (testing "date"
    (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                   (Instant/parse)
                   (.toEpochMilli)
                   (Date.))
          buf (pg/encode-bin val1 oid/date)
          val2 (pg/decode-bin buf oid/date)]
      (is (= (LocalDate/parse "2023-07-25") val2))))

  (testing "timestamp"
    (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                   (Instant/parse)
                   (.toEpochMilli)
                   (Date.))
          buf (pg/encode-bin val1 oid/timestamp)
          val2 (pg/decode-bin buf oid/timestamp)]
      (is (= (LocalDateTime/parse "2023-07-25T01:00:00.123") val2))))

  (testing "timestamptz"
    (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                   (Instant/parse)
                   (.toEpochMilli)
                   (Date.))
          buf (pg/encode-bin val1 oid/timestamptz)
          val2 (pg/decode-bin buf oid/timestamptz)]
      (is (= (OffsetDateTime/parse "2023-07-25T01:00:00.123Z") val2)))))


(deftest test-arrays

  ;; TODO: not json?
  ;; TODO: not a map?

  (testing "plain array"
    (let [val1 [1 2 3]
          buf (pg/encode-bin val1 oid/_int8)
          val2 (pg/decode-bin buf oid/_int8)]
      (is (= [1 2 3] val2))))

  (testing "multi-dim array"
    (let [val1 [[[1 nil 3] [4 nil 6]]
                [[3 nil 1] [9 nil 7]]]
          buf (pg/encode-bin val1 oid/_int8)
          val2 (pg/decode-bin buf oid/_int8)]
      (is (= val1 val2))))

  (testing "multi-dim string"
    (let [val1 [[["hello" nil "foo"] ["a" nil "b"]]
                [["aa" nil "bb"] [nil "foo" nil]]]
          buf (pg/encode-bin val1 oid/_text)
          val2 (pg/decode-bin buf oid/_text)]
      (is (= val1 val2))))

  (testing "multi-dim bool"
    (let [val1 [[[true nil false] [false nil true]]
                [[true true nil] [false false nil]]]
          buf (pg/encode-bin val1 oid/_bool)
          val2 (pg/decode-bin buf oid/_bool)]
      (is (= val1 val2))))

  (testing "multi-dim uuid"
    (let [val1 [[#uuid "3264e20e-47d6-4a5f-babd-4cf965b333ef" nil]
                [nil #uuid "1281b0d7-339a-4a91-8602-6a25c760b786"]]
          buf (pg/encode-bin val1 oid/_uuid)
          val2 (pg/decode-bin buf oid/_uuid)]
      (is (= val1 val2))))

  (testing "multi-dim float"
    (let [val1 [[(float 1.2) nil]
                [nil (float 2.3)]]
          buf (pg/encode-bin val1 oid/_float4)
          val2 (pg/decode-bin buf oid/_float4)]
      (is (= val1 val2))))

  (testing "multi-dim date"
    (let [val1 [[(LocalDate/parse "2023-01-03") nil]
                [nil (LocalDate/parse "2024-05-13")]]
          buf (pg/encode-bin val1 oid/_date)
          val2 (pg/decode-bin buf oid/_date)]
      (is (= val1 val2))))

  (testing "multi-dim time"
    (let [val1 [[(LocalTime/parse "10:11:12") nil]
                [nil (LocalTime/parse "13:14:15")]]
          buf (pg/encode-bin val1 oid/_time)
          val2 (pg/decode-bin buf oid/_time)]
      (is (= val1 val2))))

  (testing "multi-dim timetz"
    (let [val1 [[(OffsetTime/parse "10:11:12+06:30") nil]
                [nil (OffsetTime/parse "09:12:13+09:30")]]
          buf (pg/encode-bin val1 oid/_timetz)
          val2 (pg/decode-bin buf oid/_timetz)]
      (is (= val1 val2))))

  (testing "multi-dim timestamp"
    (let [val1 [[(LocalDateTime/parse "2023-01-09T10:11:12") nil]
                [nil (LocalDateTime/parse "2025-02-27T11:12:13")]]
          buf (pg/encode-bin val1 oid/_timestamp)
          val2 (pg/decode-bin buf oid/_timestamp)]
      (is (= val1 val2))))

  (testing "multi-dim timestamptz"
    (let [val1 [[(OffsetDateTime/parse "2023-01-09T10:11:12Z") nil]
                [nil (OffsetDateTime/parse "2025-02-27T11:12:13+09:30")]]
          buf (pg/encode-bin val1 oid/_timestamptz)
          val2 (pg/decode-bin buf oid/_timestamptz)]
      (is (= [[(OffsetDateTime/parse "2023-01-09T10:11:12Z") nil]
              [nil (OffsetDateTime/parse "2025-02-27T01:42:13Z")]]
             val2))))

  (testing "multi-dim numeric"
    (let [val1 [[(bigdec 123.456) nil]
                [nil (bigdec 456.789)]]
          buf (pg/encode-bin val1 oid/_numeric)
          val2 (pg/decode-bin buf oid/_numeric)]
      (is (= val1 val2)))))
