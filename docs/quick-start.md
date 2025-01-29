# Quick start (Demo)

This is a very brief passage through the library:

~~~clojure
(ns pg.demo
  (:require
   [pg.core :as pg]))

;; declare a minimal config
(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :database "test"})


;; connect to the database
(def conn
  (pg/connect config))


;; a trivial query
(pg/query conn "select 1 as one")
;; [{:one 1}]


;; let's create a table
(pg/query conn "
create table demo (
  id serial primary key,
  title text not null,
  created_at timestamp with time zone default now()
)")
;; {:command "CREATE TABLE"}


;; Insert three rows returning all the columns.
;; Pay attention that PG2 uses not question marks (?)
;; but numered dollars for parameters:
(pg/execute conn
            "insert into demo (title) values ($1), ($2), ($3)
             returning *"
            {:params ["test1" "test2" "test3"]})


;; The result: pay attention we got the j.t.OffsetDateTime class
;; for the timestamptz column (truncated):

[{:title "test1",
  :id 4,
  :created_at #object[j.t.OffsetDateTime "2024-01-17T21:57:58..."]}
 {:title "test2",
  :id 5,
  :created_at #object[j.t.OffsetDateTime "2024-01-17T21:57:58..."]}
 {:title "test3",
  :id 6,
  :created_at #object[j.t.OffsetDateTime "2024-01-17T21:57:58..."]}]


;; Try two expressions in a single transaction
(pg/with-transaction [tx conn]
  (pg/execute tx
              "delete from demo where id = $1"
              {:params [3]})
  (pg/execute tx
              "insert into demo (title) values ($1)"
              {:params ["test4"]}))
;; {:inserted 1}


;; Now check out the database log:

;; LOG:  statement: BEGIN
;; LOG:  execute s3/p4: delete from demo where id = $1
;; DETAIL:  parameters: $1 = '3'
;; LOG:  execute s5/p6: insert into demo (title) values ($1)
;; DETAIL:  parameters: $1 = 'test4'
;; LOG:  statement: COMMIT
~~~
