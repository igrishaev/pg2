(ns pg.ragtime
  (:require
   [ragtime.core :as ragtime]
   [ragtime.strategy]
   [ragtime.protocols]
   [pg.core :as pg])
  (:import org.pg.Connection))

;; TODO do not hard-code table name.
;;
;; Requires a way to generate the SQL string without risking SQL injections.
;; Perhaps use pg-honey?
(defn ensure-migrations-table!
  [conn]
  (pg/query conn "
create table if not exists ragtime_migrations(
  id text primary key,
  timestamp timestamp default current_timestamp
)"))

(defn dangerously-delete-migrations-table!!
  [conn]
  (pg/query conn "drop table ragtime_migrations"))

(extend-type Connection
  ragtime.protocols/DataStore
  (add-migration-id [conn id]
    (pg/execute conn "insert into ragtime_migrations(id) values ($1)" {:params [id]}))
  (remove-migration-id [conn id]
    (pg/execute conn "delete from ragtime_migrations where id = $1", {:params [id]}))
  (applied-migration-ids [conn]
    (->> (pg/query conn "select id from ragtime_migrations")
         (map :id))))

(comment
  ;; run
  ;;
  ;;     docker-compose up
  ;;
  ;; in pg-ragtime/ first.
  (def config {:host "localhost"
               :port 10170
               :user "test"
               :password "test"
               :database "test"})
  (def conn (pg/connect config))

  conn
  ;; => <PG connection test@localhost:10170/test>

  (instance? Connection conn)
  ;; => true

  ;; Next steps:
  ;;
  ;; - Read ragtime.jdbc @ https://github.com/weavejester/ragtime/tree/bf054489b2eb23e9a22c6f04f78d2e9f26e4dd23/jdbc/src/ragtime/jdbc.clj
  ;; - Discover what ragtime.jdbc can do that pg.ragtime currently can not
  ;; - Write example REPL code that _uses_ those features
  ;; - Implement those features.

  ;; ## Read ragtime.jdbc public interface / doc
  ;;
  ;; Source: https://weavejester.github.io/ragtime/ragtime.jdbc.html
  ;;
  ;; - (load-directory path)
  ;;   Load a collection of Ragtime migrations from a directory.
  ;;
  ;; - (load-files files)
  ;;   Given an collection of files with the same extension, return a ordered
  ;;   collection of migrations. Dispatches on extension (e.g. ".edn"). Extend
  ;;   this multimethod to support new formats for specifying SQL migrations.
  ;;
  ;; - (load-resources path)
  ;;   Load a collection of Ragtime migrations from a classpath prefix.
  ;;
  ;; - (sql-database db-spec)
  ;;   (sql-database db-spec options)
  ;;
  ;;   Given a db-spec and a map of options, return a Migratable database.
  ;;   The following options are allowed:
  ;;
  ;;   :migrations-table - the name of the table to store the applied migrations
  ;;                       (defaults to ragtime_migrations)
  ;;
  ;; - (sql-migration migration-map)
  ;;
  ;;   Create a Ragtime migration from a map with a unique :id, and :up and :down
  ;;   keys that map to ordered collection of SQL strings.
  )
