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


(deftest test-simple

  (pg/with-connection [conn CONFIG]

    (let [result
          (try-select conn {:aaa 123}

                      {:sdfsfds 55}

                      )
          ]

      (is (= 1 result))))


  )
