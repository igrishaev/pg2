(ns pg.migration-test
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
    (pgh/query conn {:drop-table [:if-exists TABLE]})
    (t)))


(use-fixtures :each fix-prepare-table)


(deftest test-migration-migrate-all
  (mig/migrate-all CONFIG)
  (pg/with-connection [conn CONFIG]
    (let [query
          {:select [:id :slug] :from TABLE :order-by [:id]}
          res
          (pgh/query conn query)]
      (is (= [{:slug "create users", :id 1}
              {:slug "create profiles", :id 2}
              {:slug "next only migration", :id 3}
              {:slug "add some table", :id 5}] res)))))

;; add more migrations
;; next only
;; down only
;; wrong pattern
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
