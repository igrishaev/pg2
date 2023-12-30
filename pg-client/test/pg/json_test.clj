(ns pg.json-test
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
