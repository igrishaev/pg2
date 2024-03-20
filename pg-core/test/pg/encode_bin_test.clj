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

  (let [val1 (OffsetTime/now)
        buf (pg/encode-bin val1 oid/timetz)
        val2 (pg/decode-bin buf oid/timetz)]
    (is (= val1 val2)))

  (let [val1 (OffsetTime/parse "12:28:23.336188+03:00")
        buf (pg/encode-bin val1 oid/time)
        val2 (pg/decode-bin buf oid/time)]
    (is (= (LocalTime/parse "12:28:23.336188") val2)))

  ;; LocalTime

  (let [val1 (LocalTime/now)
        buf (pg/encode-bin val1 oid/time)
        val2 (pg/decode-bin buf oid/time)]
    (is (= val1 val2)))

  (let [val1 (LocalTime/parse "12:41:00.005652")
        buf (pg/encode-bin val1 oid/timetz)
        val2 (pg/decode-bin buf oid/timetz)]
    (is (= (OffsetTime/parse "12:41:00.005652Z") val2)))

  ;; LocalDate

  (let [val1 (LocalDate/now)
        buf (pg/encode-bin val1 oid/date)
        val2 (pg/decode-bin buf oid/date)]
    (is (= val1 val2)))

  (let [val1 (LocalDate/parse "2022-01-01")
        buf (pg/encode-bin val1 oid/timestamp)
        val2 (pg/decode-bin buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2022-01-01T00:00") val2)))

  (let [val1 (LocalDate/parse "2022-01-01")
        buf (pg/encode-bin val1 oid/timestamptz)
        val2 (pg/decode-bin buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2022-01-01T00:00Z") val2)))

  ;; OffsetDateTime

  (let [val1 (OffsetDateTime/now)
        buf (pg/encode-bin val1 oid/timestamptz)
        val2 ^OffsetDateTime (pg/decode-bin buf oid/timestamptz)]
    (is (instance? OffsetDateTime val2))
    (is (= (.atZoneSameInstant val1 ZoneOffset/UTC)
           (.atZoneSameInstant val2 ZoneOffset/UTC))))

  (let [val1 (OffsetDateTime/parse "2023-07-27T12:44:20.611698+03:00")
        buf (pg/encode-bin val1 oid/timestamp)
        val2 (pg/decode-bin buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-27T09:44:20.611698") val2)))

  (let [val1 (OffsetDateTime/parse "2023-07-27T12:44:20.611698+03:00")
        buf (pg/encode-bin val1 oid/date)
        val2 (pg/decode-bin buf oid/date)]
    (is (= (LocalDate/parse "2023-07-27") val2)))

  ;; ZonedDateTime

  (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
        buf (pg/encode-bin val1 oid/timestamptz)
        val2 (pg/decode-bin buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2023-07-26T22:56:35.028508Z") val2)))

  (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
        buf (pg/encode-bin val1 oid/timestamp)
        val2 (pg/decode-bin buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-26T22:56:35.028508") val2)))

  (let [val1 (ZonedDateTime/parse "2023-07-27T01:56:35.028508+03:00[Europe/Moscow]")
        buf (pg/encode-bin val1 oid/date)
        val2 (pg/decode-bin buf oid/date)]
    (is (= (LocalDate/parse "2023-07-27") val2)))

  ;; Instant

  (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
        buf (pg/encode-bin val1 oid/timestamptz)
        val2 (pg/decode-bin buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2023-07-27T01:25:55.297834Z") val2)))

  (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
        buf (pg/encode-bin val1 oid/timestamp)
        val2 (pg/decode-bin buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-27T01:25:55.297834") val2)))

  (let [val1 (Instant/parse "2023-07-27T01:25:55.297834Z")
        buf (pg/encode-bin val1 oid/date)
        val2 (pg/decode-bin buf oid/date)]
    (is (= (LocalDate/parse "2023-07-27") val2)))

  ;; LocalDateTime

  (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
        buf (pg/encode-bin val1 oid/timestamptz)
        val2 (pg/decode-bin buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2023-07-27T01:31:21.025913Z") val2)))

  (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
        buf (pg/encode-bin val1 oid/timestamp)
        val2 (pg/decode-bin buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-27T01:31:21.025913") val2)))

  (let [val1 (LocalDateTime/parse "2023-07-27T01:31:21.025913")
        buf (pg/encode-bin val1 oid/date)
        val2 (pg/decode-bin buf oid/date)]
    (is (= (LocalDate/parse "2023-07-27") val2)))

  ;; Date

  (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                 (Instant/parse)
                 (.toEpochMilli)
                 (Date.))
        buf (pg/encode-bin val1 oid/date)
        val2 (pg/decode-bin buf oid/date)]
    (is (= (LocalDate/parse "2023-07-25") val2)))

  (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                 (Instant/parse)
                 (.toEpochMilli)
                 (Date.))
        buf (pg/encode-bin val1 oid/timestamp)
        val2 (pg/decode-bin buf oid/timestamp)]
    (is (= (LocalDateTime/parse "2023-07-25T01:00:00.123") val2)))

  (let [val1 (-> "2023-07-25T01:00:00.123456Z"
                 (Instant/parse)
                 (.toEpochMilli)
                 (Date.))
        buf (pg/encode-bin val1 oid/timestamptz)
        val2 (pg/decode-bin buf oid/timestamptz)]
    (is (= (OffsetDateTime/parse "2023-07-25T01:00:00.123Z") val2))))


;; TODO: implement arrays
#_
(deftest test-arrays

  ;; simple
  (let [val1 [1 2 3]
        buf (pg/encode-bin val1)
        val2 (pg/decode-bin buf oid/_int8)]
    (is (= [1 2 3] val2)))

  ;; multi-dim
  (let [val1 [[[1 nil 3] [4 nil 6]]
              [[3 nil 1] [9 nil 7]]]
        buf (pg/encode-bin val1)
        val2 (pg/decode-bin buf oid/_int8)]
    (is (= [[[1 nil 3] [4 nil 6]]
            [[3 nil 1] [9 nil 7]]]
           val2)))

  ;; string
  ;; bools
  ;; uuids
  ;; floats
  ;; dates
  ;; time
  ;; datetime
  ;; numeric

  )
