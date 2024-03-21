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


(deftest test-prepare-statement

  (testing "prepare explicit param"
    (pg/with-connection [conn CONFIG]
      (let [stmt
            (pgh/prepare conn
                         {:select [:*]
                          :from TABLE
                          :where [:= :id 0]}
                         {:oids [oid/int8]})
            res
            (pg/execute-statement conn stmt {:params [3]
                                             :first? true})]
        (is (= "<Prepared statement, name: s1, param(s): 1, OIDs: [INT8], SQL: SELECT * FROM test003 WHERE id = $1>"
               (str stmt)))
        (is (= {:name "Juan", :active true, :id 3}
               res)))))

  (testing "prepare raw param"
    (pg/with-connection [conn CONFIG]
      (let [stmt
            (pgh/prepare conn {:select [:*]
                               :from TABLE
                               :where [:raw "id = $1"]})
            res
            (pg/execute-statement conn stmt {:params [3]
                                             :first? true})]
        (is (= "<Prepared statement, name: s1, param(s): 1, OIDs: [INT4], SQL: SELECT * FROM test003 WHERE id = $1>"
               (str stmt)))
        (is (= {:name "Juan", :active true, :id 3}
               res))))))


(deftest test-query
  (pg/with-connection [conn CONFIG]
    (let [res
          (pgh/query conn
                     {:select [:id]
                      :from TABLE
                      :where [:raw "active"]
                      :limit [:raw 1]}
                     {:fn-key identity
                      :first? true})]
      (is (= {"id" 1} res)))))


(deftest test-queries
  (pg/with-connection [conn CONFIG]
    (let [res
          (pgh/queries conn
                       [{:select [:id]
                         :from TABLE
                         :where [:raw "active"]
                         :limit [:raw 1]}
                        {:select [:name]
                         :from TABLE
                         :where [:raw "active"]
                         :limit [:raw 1]}]
                       {:fn-key identity
                        :first? true})]
      (is (= [{"id" 1} {"name" "Ivan"}]
             res)))))


(deftest test-execute
  (pg/with-connection [conn CONFIG]
    (let [res
          (pgh/execute conn
                       {:select [:id]
                        :from TABLE
                        :where [:= :active true]
                        :limit 1}
                       {:fn-key identity
                        :first? true})]
      (is (= {"id" 1} res)))))


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


(deftest test-update-simple
  (testing "simple update"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/update conn TABLE {:active true})]
        (is (= [{:name "Ivan", :active true, :id 1}
                {:name "Huan", :active true, :id 2}
                {:name "Juan", :active true, :id 3}]
               res))))))


(deftest test-update-with-options
  (testing "update with options"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/update conn
                        TABLE
                        {:active true}
                        {:where [:= :name "Ivan"]
                         :returning [:id]})]
        (is (= [{:id 1}]
               res))))))


(deftest test-update-honey-expressions
  (testing "update with options"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/update conn
                        TABLE
                        {:id [:+ :id 100]
                         :active [:not :active]}
                        {:where [:= :name "Ivan"]
                         :returning [:id :active]})]
        (is (= [{:id 101, :active false}]
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


(deftest test-insert-one

  (testing "simple insert"
    (pg/with-connection [conn CONFIG]
      (let [row
            {:id 4 :name "Kyky" :active true}
            res
            (pgh/insert-one conn TABLE row)]
        (is (= {:name "Kyky", :active true, :id 4}
               res)))))

  (testing "insert with opts"
    (pg/with-connection [conn CONFIG]
      (let [row
            {:id 4 :name "Kyky" :active true}
            res
            (pgh/insert-one conn
                            TABLE
                            row
                            {:returning [:id]
                             :fn-key identity})]
        (is (= {"id" 4} res)))))

  (testing "insert return nothing"
    (pg/with-connection [conn CONFIG]
      (let [row
            {:id 4 :name "Kyky" :active true}
            res
            (pgh/insert-one conn
                            TABLE
                            row
                            {:returning nil
                             :fn-key identity})]
        (is (= {:inserted 1} res))))))


(deftest test-delete-all
  (testing "delete all"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/delete conn TABLE)
            data
            (pg/query conn "select * from test003")]
        (is (= [{:name "Ivan", :active true, :id 1}
                {:name "Huan", :active false, :id 2}
                {:name "Juan", :active true, :id 3}]
               res))
        (is (= [] data))))))


(deftest test-delete-with-extra-opts
  (testing "delete +where +returning +opts"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/delete conn TABLE
                        {:where [:or [:= :id 1] [:= :id 3]]
                         :returning [:name]
                         :fn-key identity})
            data
            (pg/query conn "select * from test003")]
        (is (= [{"name" "Ivan"}
                {"name" "Juan"}]
               res))
        (is (= [{:name "Huan", :active false, :id 2}] data))))))


(deftest test-find

  (testing "simple find"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/find conn TABLE
                      {:active true})]
        (is (= [{:name "Ivan", :active true, :id 1}
                {:name "Juan", :active true, :id 3}]
               res)))))

  (testing "find with options"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/find conn TABLE
                      {:active true}
                      {:fields [:id :name]
                       :limit 10
                       :offset 1
                       :order-by [[:id :desc]]
                       :fn-key identity})]
        (is (= [{"id" 1, "name" "Ivan"}]
               res))))))


(deftest test-find-first

  (testing "simple find first"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/find-first conn TABLE
                            {:active true})]
        (is (= {:name "Ivan", :active true, :id 1}
               res)))))

  (testing "find fist with options"
    (pg/with-connection [conn CONFIG]
      (let [res
            (pgh/find-first conn TABLE
                            {:active true}
                            {:fields [:id :name]
                             :offset 1
                             :order-by [[:id :desc]]
                             :fn-key identity})]
        (is (= {"id" 1, "name" "Ivan"}
               res))))))
