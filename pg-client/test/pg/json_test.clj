(ns pg.json-test
  (:import
   java.time.LocalDate
   java.time.LocalTime
   java.time.OffsetDateTime
   java.time.Instant)
  (:require
   [pg.bb :as bb]
   [pg.oid :as oid]
   [pg.client :as pg]
   [clojure.test :refer [deftest is]]))


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


(deftest test-json-write-string

  (let [string
        (pg/json-write-string
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
        (pg/json-read-string string)]

    (is (= [nil 1 "test" true false 1.2 "foo" {:foo 42}
            "kek"
            "2023-03-10"
            "10:30"
            "2024-01-13T22:04:50.029591+03:00"]
           data))))
