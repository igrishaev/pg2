(ns pg.honey-integration-test
  (:import
   org.pg.error.PGErrorResponse)
  (:require
   [clojure.string :as str]
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


(deftest test-get-by-ids

  (testing "hit by id"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/get-by-ids conn TABLE [1 3])]
        (is (= [{:name "Ivan", :active true, :id 1}
                {:name "Juan", :active true, :id 3}]
               res)))))

  (testing "partial hit by id"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/get-by-ids conn TABLE [1 999])]
        (is (= [{:name "Ivan", :active true, :id 1}]
               res)))))

  (testing "empty ids"
    (pg/with-connection [conn CONFIG]
      (try
        (pgh/get-by-ids conn TABLE [])
        (catch PGErrorResponse e
          (is (str/includes?
               (ex-message e)
               "syntax error at or near"))))))

  (testing "extra options"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/get-by-ids conn
                            TABLE
                            [1 2 3]
                            {:fn-key identity
                             :fields [:id]
                             :order-by [[:id :desc]]})]
        (is (= [{"id" 3} {"id" 2} {"id" 1}]
               res))))))


(deftest test-insert

  (testing "simple insert"
    (pg/with-connection [conn CONFIG]
      (let [maps
            [{:id 4 :name "Kyky" :active true}
             {:id 5 :name "Koko" :active false}
             {:id 6 :name "Kaka" :active true}]
            res
            (pgh/insert conn TABLE maps)]
        (is (= [{:name "Kyky", :active true, :id 4}
                {:name "Koko", :active false, :id 5}
                {:name "Kaka", :active true, :id 6}]
               res)))))

  (testing "insert with opts"
    (pg/with-connection [conn CONFIG]
      (let [maps
            [{:id 4 :name "Kyky" :active true}
             {:id 5 :name "Koko" :active false}
             {:id 6 :name "Kaka" :active true}]
            res
            (pgh/insert conn
                        TABLE
                        maps
                        {:returning [:id]
                         :fn-key identity})]
        (is (= [{"id" 4} {"id" 5} {"id" 6}]
               res)))))

  (testing "insert return nothing"
    (pg/with-connection [conn CONFIG]
      (let [maps
            [{:id 4 :name "Kyky" :active true}
             {:id 5 :name "Koko" :active false}
             {:id 6 :name "Kaka" :active true}]
            res
            (pgh/insert conn
                        TABLE
                        maps
                        {:returning nil
                         :fn-key identity})]
        (is (= {:inserted 3} res))))))
