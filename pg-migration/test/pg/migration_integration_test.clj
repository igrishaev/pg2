(ns pg.migration-integration-test
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.honey :as pgh]
   [pg.migration.cli :as cli]
   [pg.migration.core :as mig]))


(def TABLE :migrations_test)

(def PORT 10150)
(def USER "test")
(def HOST "127.0.0.1")


(def CONFIG
  {:host HOST
   :port PORT
   :user USER
   :password USER
   :database USER
   :migrations-table TABLE
   :migrations-path "migrations"})


(def ARGS-BASE
  ["-p" (str PORT)
   "-h" HOST
   "-u" USER
   "-w" USER
   "-d" USER
   "--table" (name TABLE)])


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

  (mig/migrate-to CONFIG 1)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG 2)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG 5)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG 5)
  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG)))

  (mig/migrate-to CONFIG 1)
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
  (try
    (mig/rollback-to CONFIG -100500)
    (is false)
    (catch Error e
      (is (= "Migration -100500 doesn't exist"
             (ex-message e)))))
  (is (= [{:slug "create users", :id 1}
          {:slug "create profiles", :id 2}
          {:slug "next only migration", :id 3}
          {:slug "prev only migration", :id 4}
          {:slug "add some table", :id 5}]
         (get-db-migrations CONFIG))))


(deftest test-migrate-to-inf
  (mig/migrate-to CONFIG 3)
  (try
    (mig/migrate-to CONFIG 999)
    (is false)
    (catch Error e
      (is (= "Migration 999 doesn't exist"
             (ex-message e)))))
  (is (= [{:slug "create users", :id 1}
          {:slug "create profiles", :id 2}
          {:slug "next only migration", :id 3}]
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


(deftest test-cli-help
  (let [res
        (with-out-str
          (cli/with-exit
            (cli/main ARGS-BASE "help")))]

    (is (= "Manage migrations via CLI

Synatax:

<global options> <command> <command options>

Global options:

  -p, --port PORT          5432         Port number
  -h, --host HOST          localhost    Host name
  -u, --user USER          wzhivga      User
  -w, --password PASSWORD               Password
  -d, --database DATABASE  wzhivga      Database
      --table TABLE        :migrations  Migrations table
      --path PATH          migrations   Migrations path

Supported commands:

 - create
 - help
 - list
 - migrate
 - rollback

Command-specific help:

<command> --help

"
           res))))


(deftest test-cli-migrate-defalut

  (cli/with-exit
    (cli/main ARGS-BASE "migrate"))

  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG))))


(deftest test-cli-migrate-rollback-mixed

  ;; +1

  (cli/with-exit
    (cli/main ARGS-BASE "migrate" "--one"))

  (is (= [{:id 1 :slug "create users"}]
         (get-db-migrations CONFIG)))

  ;; +1

  (cli/with-exit
    (cli/main ARGS-BASE "migrate" "--one"))

  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}]
         (get-db-migrations CONFIG)))

  ;; -1

  (cli/with-exit
    (cli/main ARGS-BASE "rollback" "--one"))

  (is (= [{:id 1 :slug "create users"}]
         (get-db-migrations CONFIG)))

  ;; -1

  (cli/with-exit
    (cli/main ARGS-BASE "rollback"))

  (is (= []
         (get-db-migrations CONFIG)))

  ;; +3

  (cli/with-exit
    (cli/main ARGS-BASE "migrate" "--to" "3"))

  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}]
         (get-db-migrations CONFIG)))

  ;; -2

  (cli/with-exit
    (cli/main ARGS-BASE "rollback" "--to" "1"))

  (is (= [{:id 1 :slug "create users"}]
         (get-db-migrations CONFIG))))


(deftest test-cli-help-subcommands

  (let [out
        (with-out-str
          (cli/with-exit
            (cli/main ARGS-BASE "migrate" "--help")))]

    (is (= "Syntax:
      --all           Migrate all the pending migrations
      --one           Migrate next a single pending migration
      --to ID         Migrate next to certain migration
      --help   false  Show help message
"
           out)))

  (let [out
        (with-out-str
          (cli/with-exit
            (cli/main ARGS-BASE "rollback" "--help")))]

    (is (= "Syntax:
      --all           Rollback all the previous migrations
      --one           Rollback to the previous migration
      --to ID         Rollback to certain migration
      --help   false  Show help message
"
           out)))

  (let [out
        (with-out-str
          (cli/with-exit
            (cli/main ARGS-BASE "create" "--help")))]

    (is (= "Syntax:
      --id ID             The id of the migration (auto-generated if not set)
      --slug SLUG         Optional slug (e.g. 'create-users-table')
      --help       false  Show help message
"
           out))))


(deftest test-cli-list-command

  (let [out
        (with-out-str
          (cli/with-exit
            (cli/main ARGS-BASE "list")))]

    (is (= "Migrations:

|    ID | Applied? | Slug
| ----- | -------- | --------
|     1 | false    | create users
|     2 | false    | create profiles
|     3 | false    | next only migration
|     4 | false    | prev only migration
|     5 | false    | add some table
"
           out)))

  (cli/with-exit
    (cli/main ARGS-BASE  "migrate"))

  (is (= [{:id 1 :slug "create users"}
          {:id 2 :slug "create profiles"}
          {:id 3 :slug "next only migration"}
          {:id 4 :slug "prev only migration"}
          {:id 5 :slug "add some table"}]
         (get-db-migrations CONFIG)))

  (let [out
        (with-out-str
          (cli/with-exit
            (cli/main ARGS-BASE "list")))]

    (is (= "Migrations:

|    ID | Applied? | Slug
| ----- | -------- | --------
|     1 | true     | create users
|     2 | true     | create profiles
|     3 | true     | next only migration
|     4 | true     | prev only migration
|     5 | true     | add some table
"
           out))))
