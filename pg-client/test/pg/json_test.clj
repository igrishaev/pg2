(ns pg.json-test
  (:import
   java.io.StringWriter
   java.io.StringReader
   java.io.ByteArrayOutputStream
   java.io.ByteArrayInputStream
   java.time.Instant
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetDateTime)
  (:require
   [clojure.test :refer [deftest is]]
   [pg.bb :as bb]
   [pg.core :as pg]
   [pg.json :as json]
   [pg.oid :as oid]))


(deftest test-json-txt

  (let [data
        {:foo [1 2 3]}

        encoded
        (pg/encode-txt data oid/json)

        decoded
        (pg/decode-txt encoded oid/jsonb)]

    (is (string? encoded))
    (is (= data decoded))))


(deftest test-json-txt-string

  (let [init
        "[1, 2, 3]"

        encoded
        (pg/encode-txt init oid/json)]

    (is (= init encoded))))


(deftest test-json-bin

  (let [data
        {:foo [1 2 3]}

        encoded
        (pg/encode-bin data oid/json)

        decoded
        (pg/decode-bin encoded oid/jsonb)]

    (is (bb/bb? encoded))
    (is (= data decoded))))


(deftest test-json-bin-string

  (let [data
        "[1, 2, 3]"

        encoded
        (pg/encode-bin data oid/json)]

    (is (bb/bb? encoded))
    (is (= data (new String (.array encoded) "UTF-8")))))


(deftest test-json-read-&-write-string

  (let [string
        (json/write-string
         [nil
          1
          "test"
          true
          false
          1.2
          :foo
          {:foo 42}
          'kek
          (LocalDate/parse "2023-03-10")
          (LocalTime/parse "10:30")
          (OffsetDateTime/parse "2024-01-13T22:04:50.029591+03:00")])

        data
        (json/read-string string)]

    (is (= [nil 1 "test" true false 1.2 "foo" {:foo 42}
            "kek"
            "2023-03-10"
            "10:30"
            "2024-01-13T22:04:50.029591+03:00"]
           data))))


(deftest test-json-read-&-write-stream

  (let [data1
        [1 2 3]

        out
        (new ByteArrayOutputStream)

        _
        (json/write-stream data1 out)

        in
        (new ByteArrayInputStream (.toByteArray out))

        data2
        (json/read-stream in)]

    (is (= data1 data2))))


(deftest test-json-read-&-write-reader-writer

  (let [data1
        [1 2 3]

        writer
        (new StringWriter)

        _
        (json/write-writer data1 writer)

        reader
        (new StringReader "[1,2,3]")

        data2
        (json/read-reader reader)]

    (is (= "[1,2,3]" (str writer)))
    (is (= [1 2 3] data2))))
