(ns pg.honey-integration-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.oid :as oid]
   [pg.honey :as pgh]
   [pg.pool :as pool]))


(def CONFIG
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})


(def TABLE :test003)


(defn fix-prepare-table
  [t]
  (pg/with-connection [conn CONFIG]
    (pg/query conn "create table test003 (id integer not null, name text not null, active boolean not null default true)")
    (pg/query conn "insert into test003 (id, name, active) values (1, 'Ivan', true), (2, 'Huan', false), (3, 'Juan', true)")
    (t)
    (pg/query conn "drop table test003")))


(use-fixtures :each fix-prepare-table)


(deftest test-get-by-id

  (testing "hit by id"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/get-by-id conn TABLE 1)]
        (is (= {:name "Ivan" :active true :id 1}
               res)))))

  (testing "miss by id"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/get-by-id conn TABLE 999)]
        (is (nil? res)))))

  (testing "various params"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/get-by-id conn
                           TABLE
                           3
                           {:pk [:raw "test003.id"]
                            :fields [:name :active]})]
        (is (= {:name "Juan", :active true}
               res))))))
