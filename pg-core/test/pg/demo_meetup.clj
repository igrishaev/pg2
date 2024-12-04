(ns pg.demo-meetup
  (:require
   [pg.core :as pg]))


(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})

(def conn
  (pg/connect config))

(pg/query conn "select * from generate_series(1, 9) as seq(x)")

(pg/query conn "select
x as num,
x * x as square,
'this is ' || x as string,
current_timestamp
from generate_series(1, 9) as seq(x)"
          )



- разные типы полей (дата, время, json)
- транзакция (вложенная)
- copy-in/out
- SSL с сертификатом
- массивы
- отладочный лог
- ?
