(ns pg.encode-txt-test
  (:import
   java.math.BigDecimal
   java.math.BigInteger
   java.time.Instant
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetDateTime
   java.time.OffsetTime
   java.time.ZonedDateTime
   java.util.Date
   org.pg.error.PGError)
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as j]
   [pg.core :as pg]
   [pg.oid :as oid]))


(defn reverse-string [s]
  (apply str (reverse s)))


(def custom-mapper
  (j/object-mapper
   {:encode-key-fn (comp reverse-string name)
    :decode-key-fn (comp keyword reverse-string)}))


(deftest test-bytea
  (let [res (pg/encode-txt (.getBytes "hello" "UTF-8"))]
    (is (= "\\x68656c6c6f" res))))


(deftest test-char-types
  (doseq [oid [oid/text
               oid/char
               oid/varchar
               oid/bpchar]]
    (is (= "test" (pg/encode-txt "test" oid)))))


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


(deftest test-json-custom-mapper
  (let [string
        (pg/encode-txt {:foo 123}
                       oid/json
                       {:object-mapper custom-mapper})

        data
        (j/read-value string)]

    (is (= {"oof" 123} data))))


(deftest test-json-encode-txt

  (testing "json coll"
    (let [string (pg/encode-txt [1 2 3] oid/json)]
      (is (= "[1,2,3]" string))))

  (testing "jsonb coll"
    (let [string (pg/encode-txt [1 2 3] oid/jsonb)]
      (is (= "[1,2,3]" string))))

  (testing "default coll error"
    (try
      (pg/encode-txt [1 2 3])
      (is false "must not be reached")
      (catch PGError e
        (is (-> e
                ex-message
                (str/starts-with? "cannot text-encode a value: [1 2 3], OID: DEFAULT"))))))

  (testing "json string"
    (let [string (pg/encode-txt "[1,2,3]" oid/json)]
      (is (= "[1,2,3]" string))))

  (testing "jsonb string"
    (let [string (pg/encode-txt "[1,2,3]" oid/jsonb)]
      (is (= "[1,2,3]" string))))

  (testing "jsonb number"
    (let [string (pg/encode-txt 1 oid/jsonb)]
      (is (= "1" string))))

  (testing "json number"
    (let [string (pg/encode-txt 1 oid/json)]
      (is (= "1" string))))

  (testing "default wrapper number"
    (let [string (pg/encode-txt (pg/json-wrap 1))]
      (is (= "1" string))))

  (testing "json wrapper number"
    (let [string (pg/encode-txt (pg/json-wrap 1) oid/json)]
      (is (= "1" string)))))


(deftest test-array-encode-txt

  (testing "plain"
    (let [result
          (pg/encode-txt [1 2 3]
                         oid/_int4)]
      (is (= "{\"1\",\"2\",\"3\"}" result))))

  (testing "multi-dim"
    (let [result
          (pg/encode-txt [["aa" nil "bb"]
                          ["cc" nil "dd"]]
                         oid/_text)]
      (is (= "{{\"aa\",NULL,\"bb\"},{\"cc\",NULL,\"dd\"}}" result))))

  (testing "numbers"
    (doseq [oid [oid/_int2
                 oid/_int4
                 oid/_int8
                 oid/_oid
                 oid/_float4
                 oid/_float8
                 oid/_numeric]]
      (let [result
            (pg/encode-txt [[1 nil 2]
                            [3 nil 4]]
                           oid)]
        (is (= "{{\"1\",NULL,\"2\"},{\"3\",NULL,\"4\"}}" result)))))

  (testing "text"
    (doseq [oid [oid/_text
                 oid/_varchar
                 oid/_name
                 oid/_char
                 oid/_bpchar]]
      (let [result
            (pg/encode-txt [["a\\a" nil "b\"b"]
                            ["cc" nil "dd"]]
                           oid)]
        (is (= "{{\"a\\\\a\",NULL,\"b\\\"b\"},{\"cc\",NULL,\"dd\"}}" result)))))

  (testing "uuid"
    (doseq [oid [oid/_text
                 oid/_uuid]]
      (let [result
            (pg/encode-txt [[#uuid "1bce0361-8791-4923-b3bc-9002c5498a24" nil]
                            [nil #uuid "5abe9c84-266e-48b3-91b6-f344c61c660b"]] oid)]
        (is (= "{{\"1bce0361-8791-4923-b3bc-9002c5498a24\",NULL},{NULL,\"5abe9c84-266e-48b3-91b6-f344c61c660b\"}}"
               result)))))

  (testing "bool"
    (let [result
          (pg/encode-txt [[true nil false]
                          [false nil true]]
                         oid/_bool)]
      (is (= "{{\"t\",NULL,\"f\"},{\"f\",NULL,\"t\"}}" result))))

  (testing "bool"
    (let [result
          (pg/encode-txt [[true nil false]
                          [false nil true]]
                         oid/_bool)]
      (is (= "{{\"t\",NULL,\"f\"},{\"f\",NULL,\"t\"}}" result))))

  (testing "json(b)"
    (doseq [oid [oid/_json
                 oid/_jsonb]]
      (let [result
            (pg/encode-txt [[{:foo 1} nil {:bar "test"}]
                            [(pg/json-wrap [1, 2, 3]) nil true]]
                           oid)]
        (is (= "{{\"{\\\"foo\\\":1}\",NULL,\"{\\\"bar\\\":\\\"test\\\"}\"},{\"[1,2,3]\",NULL,\"true\"}}" result)))))


  (testing "time"
    (doseq [oid [oid/_time]]
      (let [result
            (pg/encode-txt [[(LocalTime/parse "10:30") nil]
                            [(LocalTime/parse "12:30") nil]]
                           oid)]
        (is (= "{{\"10:30:00.000000\",NULL},{\"12:30:00.000000\",NULL}}" result)))))

  (testing "timetz"
    (doseq [oid [oid/_timetz]]
      (let [result
            (pg/encode-txt [[(OffsetTime/parse "10:30+04:30") nil]
                            [(OffsetTime/parse "12:30-02:30") nil]]
                           oid)]
        (is (= "{{\"10:30:00.000000+0430\",NULL},{\"12:30:00.000000-0230\",NULL}}" result)))))

  (testing "date"
    (doseq [oid [oid/_date]]
      (let [result
            (pg/encode-txt [[(LocalDate/parse "2024-01-03") nil]
                            [(LocalDate/parse "2025-03-19") nil]]
                           oid)]
        (is (= "{{\"2024-01-03\",NULL},{\"2025-03-19\",NULL}}" result)))))

  (testing "timestamp"
    (doseq [oid [oid/_timestamp]]
      (let [result
            (pg/encode-txt [[(LocalDateTime/parse "2024-01-03T23:59:59") nil]
                            [(LocalDateTime/parse "2025-02-03T23:59:00") nil]]
                           oid)]
        (is (= "{{\"2024-01-03 23:59:59.000000\",NULL},{\"2025-02-03 23:59:00.000000\",NULL}}" result)))))

  (testing "timestamptz"
    (doseq [oid [oid/_timestamptz]]
      (let [result
            (pg/encode-txt [[(OffsetDateTime/parse "2024-01-03T23:59:59Z") nil]
                            [(OffsetDateTime/parse "2025-02-03T23:59:00-03") nil]]
                           oid)]
        (is (= "{{\"2024-01-03 23:59:59.000000+00\",NULL},{\"2025-02-04 02:59:00.000000+00\",NULL}}" result))))))
