(ns pg.honeysql-test
  (:require
   [pg.hugsql :as hugsql]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.oid :as oid]
   [pg.pool :as pool]))


(def CONFIG
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})


(hugsql/def-db-fns (io/file "test/pg/db.sql"))


(deftest test-query-select-default
  (pg/with-connection [conn CONFIG]
    (let [result
          (try-select-jsonb conn
                            {:query "params"}
                            {:command "params"})]
      (is (= [{:json {:foo 42}}]
             result)))))


(deftest test-create-test-table-simple
  (pg/with-connection [conn CONFIG]

    (let [table
          (str (gensym "tmp"))

          result
          (create-test-table conn
                             {:table table})]

      (is (= {:command "CREATE TABLE"}
             result)))))


(deftest test-insert-into-table

  (pg/with-connection [conn CONFIG]

    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          result-insert
          (insert-into-table conn {:table table
                                   :title "Hello!"})

          result-query
          (select-from-table conn {:table table})]

      (is (= 1
             result-insert))

      (is (= 1
             result-query)))))


(deftest test-insert-into-table

  (pg/with-connection [conn CONFIG]

    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          result-insert
          (insert-into-table conn {:table table
                                   :title "Hello!"})

          result-query
          (select-from-table conn {:table table})

          result-get
          (get-by-id conn {:table table :id 1})

          result-miss
          (get-by-id conn {:table table :id 999})]

      (is (= 1
             result-insert))

      (is (= [{:title "Hello!", :id 1}]
             result-query))

      (is (= {:title "Hello!", :id 1}
             result-get))

      (is (= nil result-miss))

      )))

;; todo: execute custom params
;; insert returning
;; insert n
;; select where
;; insert json
