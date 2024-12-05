(ns pg.demo-meetup
  (:require
   [clojure.java.io :as io]
   [pg.core :as pg]
   [pg.ssl :as ssl]))


(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})

(def conn
  (pg/connect config))

conn

(pg/query conn "select * from generate_series(1, 9) as seq(x)")

(pg/query conn "select
x as num,
x * x as square,
'this is ' || x as string,
current_timestamp
from generate_series(1, 9) as seq(x)"
          )

(pg/query conn "create temp table test (id integer, data jsonb)")


;; json
(pg/with-tx [conn]
  (pg/execute conn "insert into test (id, data) values ($1, $2)"
              {:params [1, {:foo "hello"}]})
  (pg/execute conn "insert into test (id, data) values ($1, $2)"
              {:params [2, {:bar [1 2 3]}]}))

(pg/query conn "drop table if exists test")


;; array

(pg/query conn "create temp table test (id integer, arr int[][])")

(pg/execute conn "insert into test (id, arr) values ($1, $2)"
              {:params [1, [[1 2] [3 4]]]})

(pg/query conn "select * from test")


;; nested tx

(pg/with-tx [conn]
  (pg/with-tx [conn]
    (pg/with-tx [conn]
      (pg/query conn "select 1"))))


;; pubsub

(def conn1
  (pg/connect (assoc config
                     :fn-notification
                     (fn [msg]
                       (clojure.pprint/pprint msg)))))


(pg/listen conn1 "test01")


(def conn2
  (pg/connect config))


(pg/notify conn2 "test01" "hey there!")

(pg/query conn1 "select")



;; SSL


(def ssl-context
  (ssl/context "/Users/ivan/Downloads/prod-ca-2021.crt"))


(def config-sb
  {:host "aws-0-eu-central-1.pooler.supabase.com"
   :port 6543
   :user "postgres.XXXX"
   :password "<password>"
   :database "postgres"
   :ssl-context ssl-context
   })

(pg/with-conn [c config-sb]
  (pg/query c "select 42 as the_answer"))

;; folders


(def QUERY
  "select
x as id,
x * x as some_field,
'this is ' || x as string
from generate_series(1, 9) as seq(x)"
  )

(pg/query conn QUERY {:index-by :id})

(pg/query conn QUERY {:columns [:id :string]})

(pg/query conn QUERY {:kv [:id :string]})


(with-open [out (io/writer "json-dump.json")]
  (pg/query conn QUERY {:to-json out}))

(with-open [out (io/writer "edn-dump.edn")]
  (pg/query conn QUERY {:to-edn out}))
