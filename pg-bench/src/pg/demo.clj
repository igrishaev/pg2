(ns pg.demo
  (:require
   [pg.core :as pg]))


#_
(comment

  (require '[pg.core :as pg])
  (require '[pg.pool :as pool])


  (def config
    {:host "127.0.0.1"
     :port 10140
     :user "test"
     :password "test"
     :database "test"})


  (def conn
    (pg/connect config))

  (str conn)

  (pg/with-connection [conn config]
    (pg/query conn "select 1 as one"))

  (with-open [conn (pg/connect config)]
    (pg/query conn "select 1 as one"))


  (pg/query conn "select 1 as one")
  [{:one 1}]


  (pg/query conn "
create table demo (
  id serial primary key,
  title text not null,
  created_at timestamp with time zone default now()
)")
  {:command "CREATE TABLE"}


  (pg/execute conn
              "insert into demo (title) values ($1), ($2), ($3)
             returning *"
              {:params ["test1" "test2" "test3"]})

  (see trash.clj)

  (pg/execute conn
              "select * from demo where id = $1"
              {:params [5]
               :first? true})


  (pg/with-tx [conn]
    (pg/execute conn
                "delete from demo where id = $1"
                {:params [3]})
    (pg/execute conn
                "insert into demo (title) values ($1)"
                {:params ["test4"]}))
  {:inserted 1}

  ;; LOG:  statement: BEGIN
  ;; LOG:  execute s3/p4: delete from demo where id = $1
  ;; DETAIL:  parameters: $1 = '3'
  ;; LOG:  execute s5/p6: insert into demo (title) values ($1)
  ;; DETAIL:  parameters: $1 = 'test4'
  ;; LOG:  statement: COMMIT




  )
