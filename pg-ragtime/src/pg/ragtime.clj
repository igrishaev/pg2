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

  )
