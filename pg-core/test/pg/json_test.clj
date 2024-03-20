(ns pg.json-test
  (:import
   java.io.ByteArrayInputStream
   java.io.ByteArrayOutputStream
   java.io.StringReader
   java.io.StringWriter
   java.time.Instant
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetDateTime)
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [jsonista.core :as j]
   [pg.bb :as bb]
   [pg.core :as pg]
   [pg.json :as json]
   [pg.oid :as oid]))


(defn reverse-string [s]
  (apply str (reverse s)))


(def custom-mapper
  (j/object-mapper
   {:encode-key-fn (comp reverse-string name)
    :decode-key-fn (comp keyword reverse-string)}))


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
        (pg/decode-bin encoded oid/json)]

    (is (bb/bb? encoded))
    (is (= data decoded))))


(deftest test-jsonb-bin

  (let [data
        {:foo [1 2 3]}

        encoded
        (pg/encode-bin data oid/jsonb)

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


;;
;; Custom mapper
;;

(deftest test-mapper-read-string
  (let [string
        (json/write-string {:foo 42})

        data
        (json/read-string custom-mapper string)]

    (is (= {:oof 42} data))))


(deftest test-mapper-read-stream
  (let [stream
        (-> {:foo 42}
            json/write-string
            (.getBytes "utf-8")
            io/input-stream)

        data
        (json/read-stream custom-mapper stream)]

    (is (= {:oof 42} data))))


(deftest test-mapper-read-reader
  (let [reader
        (-> {:foo 42}
            json/write-string
            (.getBytes "utf-8")
            io/reader)

        data
        (json/read-reader custom-mapper reader)]

    (is (= {:oof 42} data))))


(deftest test-mapper-write-string
  (let [string
        (json/write-string custom-mapper {:foo 42})

        data
        (json/read-string string)]

    (is (= {:oof 42} data))))


(deftest test-mapper-write-stream
  (let [out
        (new ByteArrayOutputStream)

        _
        (json/write-stream custom-mapper {:foo 42} out)

        in
        (-> out .toByteArray io/input-stream)

        data
        (json/read-string (slurp in))]

    (is (= {:oof 42} data))))


(deftest test-mapper-write-writer
  (let [out
        (new StringWriter)

        _
        (json/write-writer custom-mapper {:foo 42} out)

        data
        (json/read-string (str out))]

    (is (= {:oof 42} data))))
