(ns pg.ragtime
  (:require
   [ragtime.core :as ragtime]
   [ragtime.strategy]
   [ragtime.protocols]
   [pg.core :as pg])
  (:import org.pg.Connection))

(defn ensure-migrations-table!
  [conn]
  (pg/query conn "
create table if not exists migrations(
  id text primary key,
  timestamp timestamp default current_timestamp
)"))

(defn dangerously-delete-migrations-table!!
  [conn]
  (pg/query conn "drop table migrations"))

(extend-type Connection
  ragtime.protocols/DataStore
  (add-migration-id [conn id]
    (pg/execute conn "insert into migrations(id) values ($1)" {:params [id]}))
  (remove-migration-id [conn id]
    (pg/execute conn "delete from migrations where id = $1", {:params [id]}))
  (applied-migration-ids [conn]
    (->> (pg/query conn "select id from migrations")
         (map :id))))
