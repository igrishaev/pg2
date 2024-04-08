(ns pg.honeysql-test
  (:require
   [pg.hugsql :as pg.hug]
   [hugsql.core :as hugsql]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [clojure.java.io :as io]
   ;; [pg.integration :refer [*CONFIG* P15]]
   [pg.oid :as oid]
   [pg.pool :as pool])
  )


(def CONFIG
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})


(def adapter
  (pg.hug/adapter))


(hugsql/def-db-fns
  (io/file "test/pg/db.sql")
  {:adapter adapter})


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
             result-query))))
  )

;; todo: execute custom params
;; insert returning
;; insert n
;; select where
;; insert json
