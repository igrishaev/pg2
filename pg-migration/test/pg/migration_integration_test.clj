(ns pg.migration-integration-test
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.honey :as pgh]
   [pg.migration :as mig]))


(def TABLE :migrations_test)


(def CONFIG
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"
   :migrations-table TABLE
   :migrations-path "migrations"})


(defn fix-prepare-table
  [t]
  (pg/with-connection [conn CONFIG]
    (pgh/queries conn
                 [{:drop-table [:if-exists TABLE]}
                  {:drop-table [:if-exists :test_mig3]}
                  {:drop-table [:if-exists :test_mig4]}
                  {:drop-table [:if-exists :test_mig5]}])
    (t)))


(use-fixtures :each fix-prepare-table)


(defn get-db-migrations [config]
  (pg/with-connection [conn config]
    (let [query
          {:select [:id :slug] :from TABLE :order-by [:id]}]
      (pgh/query conn query))))


(deftest test-migration-migrate-all
  (mig/migrate-all CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG))))


(deftest test-migration-migrate-one

  (mig/migrate-one CONFIG)
  (is (= [{:id 1 :slug "create users"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-one CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-one CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-one CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-one CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-one CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG))))


(deftest test-migration-migrate-to

  (mig/migrate-to CONFIG 2)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG -99999)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG 2)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG 999)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG 999)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG -99999)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG))))


(deftest test-rollback-all
  (mig/migrate-all CONFIG)
  (mig/rollback-all CONFIG)
  (is (= []
         (get-db-migrations CONFIG))))


(deftest test-rollback-one

  (mig/migrate-one CONFIG)
  (mig/migrate-one CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  (mig/rollback-one CONFIG)
  (is (= [{:id 1 :slug "create users"}]
         (get-db-migrations CONFIG)))

  (mig/rollback-one CONFIG)
  (is (= []
         (get-db-migrations CONFIG)))

  (mig/migrate-one CONFIG)
  (mig/migrate-one CONFIG)
  (mig/migrate-one CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}]
         (get-db-migrations CONFIG)))

  (mig/rollback-one CONFIG)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  (mig/rollback-one CONFIG)
  (is (= [{:id 1 :slug "create users"}]
         (get-db-migrations CONFIG)))

  (mig/rollback-one CONFIG)
  (is (= []
         (get-db-migrations CONFIG))))



;; add migration with wrong pattern
;; conflicted migrations (applied before)
;; double migration file
;; migrate all
;; migrate back
;; migrate to ok
;; migrate to missing
;; migrate one at last
;; rollback all
;; rollback one
;; rollback one at the beginning
;; rollback to
;; rollback wrong
;; check jar file
;; check weird sql syntax
