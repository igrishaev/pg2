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


(deftest test-rollback-to-wrong-id

  (mig/migrate-all CONFIG)
  (mig/rollback-to CONFIG 10000)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG))))


(deftest test-rollback-to-4

  (mig/migrate-all CONFIG)
  (mig/rollback-to CONFIG 4)
  (is (= [{:slug "create users", :id 1}
          {:slug "create profiles", :id 2}
          {:slug "next only migration", :id 3}
          {:slug "prev only migration", :id 4}]
         (get-db-migrations CONFIG))))


(deftest test-rollback-to-minus

  (mig/migrate-all CONFIG)
  (mig/rollback-to CONFIG -100500)
  (is (= []
         (get-db-migrations CONFIG))))


(defmacro with-file [[path content] & body]
  `(do
     (spit ~path ~content)
     (try
       ~@body
       (finally
         (io/delete-file ~path)))))


(deftest test-migrate-weird-sql
  (let [path
        "test/resources/migrations/999-weird-syntax.next.sql"
        content
        "selekt 100500"]
    (with-file [path content]

      (try
        (mig/migrate-all CONFIG)
        (is false)
        (catch Throwable e
          (is (-> e
                  ex-message
                  (str/includes? "syntax error at or near")))))

      (is (= [{:id 1 :slug "create users"}
              {:id 2 :slug "create profiles"}
              {:id 3 :slug "next only migration"}
              {:id 4 :slug "prev only migration"}
              {:id 5 :slug "add some table"}]
             (get-db-migrations CONFIG))))))
