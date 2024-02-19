(ns pg.demo
  (:require
   [pg.core :as pg]))


(comment

  (require '[pg.pool :as pool])

  (require '[pg.jdbc :as jdbc])
  (require '[pg.oid :as oid])

  (def config
    {:host "127.0.0.1"
     :port 10140
     :user "test"
     :password "test"
     :dbname "test"})

  (def conn
    (jdbc/get-connection config))

  (jdbc/on-connection [conn config]
    (println conn))

  ;; <PG connection test@127.0.0.1:10140/test>

  (def pool (pool/pool config))

  (jdbc/on-connection [conn pool]
    (println conn))

  (jdbc/on-connection [conn config]
    (jdbc/execute! conn ["select $1 as num" 42]))

  [{:num 42}]

  (jdbc/on-connection [conn config]
    (jdbc/execute-one! conn ["select $1 as num" 42]))

  {:num 42}

  (jdbc/on-connection [conn config]
    (let [stmt
          (jdbc/prepare conn
                        ["select $1::int4 + 1 as num"])
          res1
          (jdbc/execute-one! conn [stmt 1])

          res2
          (jdbc/execute-one! conn [stmt 2])]

      [res1 res2]))

  ;; [{:num 2} {:num 3}]

  (jdbc/execute! config ["create table test2 (id serial primary key, name text not null)"])

  (jdbc/on-connection [conn config]
    (let [stmt
          (jdbc/prepare conn
                        ["insert into test2 (name) values ($1) returning *"])

          res1
          (jdbc/execute-one! conn [stmt "Ivan"])

          res2
          (jdbc/execute-one! conn [stmt "Huan"])]

      [res1 res2]))

  [{:name "Ivan", :id 1} {:name "Huan", :id 2}]

  (jdbc/on-connection [conn config]
    (let [stmt
          (jdbc/prepare conn
                        ["insert into test2 (name) values ($1) returning *"])]

      (jdbc/with-transaction [TX conn {:isolation :serializable
                                       :read-only false
                                       :rollback-only false}]

        (let [res1
              (jdbc/execute-one! conn [stmt "Snip"])

              res2
              (jdbc/execute-one! conn [stmt "Snap"])]

          [res1 res2]))))

  [{:name "Snip", :id 3} {:name "Snap", :id 4}]

  ;; BEGIN
  ;; SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
  ;; insert into test2 (name) values ($1) returning *
  ;;   $1 = 'Snip'
  ;; insert into test2 (name) values ($1) returning *
  ;;   $1 = 'Snap'
  ;; COMMIT

  (jdbc/with-transaction [TX config {:read-only true}]
    (jdbc/execute! TX ["delete from test2"]))

  (jdbc/with-transaction [TX config {:rollback-only true}]
    (jdbc/execute! TX ["delete from test2"]))

  (jdbc/execute! config ["select * from test2"])


  (jdbc/on-connection [conn config]

    (let [res1 (jdbc/active-tx? conn)]

      (jdbc/with-transaction [TX conn {:isolation :serializable
                                       :read-only false
                                       :rollback-only false}]

        (let [res2 (jdbc/active-tx? TX)]

          [res1 res2]))))

  [false true]

  (jdbc/on-connection [conn config]
    (let [res1 (jdbc/active-tx? conn)]
      (jdbc/with-transaction [TX conn]
        (let [res2 (jdbc/active-tx? TX)]
          [res1 res2]))))

  [false true]

  ;; get-connection
  ;; kebab-case, no fq yet

  (jdbc/on-connection [conn config]
    (jdbc/execute-one! conn ["select 42 as the_answer"]))

  {:the-answer 42}

  (jdbc/on-connection [conn config]
    (jdbc/execute-one! conn
                       ["select 42 as the_answer"]
                       {:kebab-keys? false}))

  {:the_answer 42}

  (require '[pg.pool :as pool])

  (pool/with-pool [pool config]
    (let [f1
          (future
            (jdbc/on-connection [conn1 pool]
              (println
               (jdbc/execute-one! conn1 ["select 'hoho' as message"]))))
          f2
          (future
            (jdbc/on-connection [conn2 pool]
              (println
               (jdbc/execute-one! conn2 ["select 'haha' as message"]))))]
      @f1
      @f2)))

  ;; {{:message hoho}:message haha}


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

  (def config+
    (assoc config
           :pg-params
           {"application_name" "Clojure"
            "DateStyle" "ISO, MDY"}))

  (def conn
    (pg/connect config+))

  (pg/query conn "create table test1 (id serial primary key, name text)")

  (pg/query conn "insert into test1 (name) values ('Ivan'), ('Huan')")

  (pg/query conn "select * from test1")

  (pg/query conn "insert into test1 (name) values ('Juan'); select * from test1")

  (pg/execute conn "select * from test1 where id = $1" {:params [2]})

  (def pairs
    [[1001 "Harry"]
     [1002 "Hermione"]
     [1003 "Ron"]])

  (flatten pairs)


  (def stmt-by-id
    (pg/prepare conn "select * from test1 where id = $1"))

  (pg/query conn "insert into test1 (name) values ('Juan'); select * from test1")

  (pg/execute-statement conn
                        stmt-by-id
                        {:params [1] :first? true})

  {:name "Ivan", :id 1}

  (pg/execute-statement conn
                        stmt-by-id
                        {:params [5] :first? true})

  {:name "Louie", :id 5}

  (pg/execute-statement conn
                        stmt-by-id
                        {:params [8] :first? true})

  {:name "Agent Smith", :id 8}

  (pg/with-statement [stmt conn "insert into test1 (name) values ($1) returning *"]
    (doall
     (for [character ["Agent Brown"
                      "Agent Smith"
                      "Agent Jones"]]
       (pg/execute-statement conn stmt {:params [character] :first? true}))))

  ({:name "Agent Brown", :id 12}
   {:name "Agent Smith", :id 13}
   {:name "Agent Jones", :id 14})

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


  ;;
  ;; Transactions
  ;;

  (pg/begin conn)

  (pg/execute conn
              "insert into test1 (name) values ($1)"
              {:params ["Test1"]})

  (pg/execute conn
              "insert into test1 (name) values ($1)"
              {:params ["Test2"]})

  (pg/commit conn)

  (pg/query conn
            "select name from test1 where name like 'Test%'")

  ;; [{:name "Test1"} {:name "Test2"}]


  ;; statement: BEGIN
  ;; execute s23/p24: insert into test1 (name) values ($1)
  ;;   parameters: $1 = 'Test1'
  ;; execute s25/p26: insert into test1 (name) values ($1)
  ;;   parameters: $1 = 'Test2'
  ;; statement: COMMIT

  (pg/begin conn)
  (pg/execute conn
              "insert into test1 (name) values ($1)"
              {:params ["Test3"]})
  (pg/rollback conn)

  (pg/query conn
            "select name from test1 where name like 'Test%'")
  ;; [{:name "Test1"} {:name "Test2"}]


  (pg/status conn)
  :I

  (pg/idle? conn)
  true

  (pg/begin conn)

  (pg/status conn)
  :T

  (pg/in-transaction? conn)
  true

  (pg/query conn "selekt dunno")

  ;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:205).
  ;; Server error response: {severity=ERROR, code=42601, file=scan.l, line=1176, function=scanner_yyerror, position=1, message=syntax error at or near "selekt", verbosity=ERROR}

  (pg/status conn)
  :E

  (pg/tx-error? conn)
  true

  (pg/rollback conn)

  (pg/idle? conn)
  true

  (pg/with-tx [conn]
    (pg/execute conn
                "delete from test1 where name like $1"
                {:params ["Test%"]})
    (pg/execute conn
                "insert into test1 (name) values ($1)"
                {:params ["Test3"]}))

  ;; auto rollback

  (pg/begin conn)
  (try
    (let [result (do <body>)]
      (pg/commit conn)
      result)
    (catch Throwable e
      (pg/rollback conn)
      (throw e)))



  ;;   statement: BEGIN
  ;; execute s29/p30: delete from test1 where name like $1
  ;;   parameters: $1 = 'Test%'
  ;; execute s31/p32: insert into test1 (name) values ($1)
  ;;   parameters: $1 = 'Test3'
  ;; statement: COMMIT

  (pg/query conn
            "select name from test1 where name like 'Test%'")
  [{:name "Test3"}]



  (pg/with-tx [conn {:isolation-level :serializable}]
    (pg/execute conn
                "delete from test1 where name like $1"
                {:params ["Test%"]})
    (pg/execute conn
                "insert into test1 (name) values ($1)"
                {:params ["Test3"]}))


  (:serializable "SERIALIZABLE")

  (:repeatable-read "REPEATABLE READ")

  (:read-committed "READ COMMITTED")

  (:read-uncommitted "READ UNCOMMITTED")
  TxLevel/READ_UNCOMMITTED

  )


;; pg14_1  | 2024-02-14 16:57:54.539 UTC [9020] LOG:  statement: BEGIN
;; pg14_1  | 2024-02-14 16:57:54.556 UTC [9020] LOG:  statement: SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
;; pg14_1  | 2024-02-14 16:57:54.594 UTC [9020] LOG:  execute s5/p6: delete from test1 where name like $1
;; pg14_1  | 2024-02-14 16:57:54.594 UTC [9020] DETAIL:  parameters: $1 = 'Test%'
;; pg14_1  | 2024-02-14 16:57:54.605 UTC [9020] LOG:  execute s7/p8: insert into test1 (name) values ($1)
;; pg14_1  | 2024-02-14 16:57:54.605 UTC [9020] DETAIL:  parameters: $1 = 'Test3'
;; pg14_1  | 2024-02-14 16:57:54.608 UTC [9020] LOG:  statement: COMMIT



#_
(comment

  (pg/with-tx [conn {:read-only? true}]
    (pg/execute conn
                "delete from test1 where name like $1"
                {:params ["Test%"]})
    (pg/execute conn
                "insert into test1 (name) values ($1)"
                {:params ["Test3"]}))

  ;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:205).
  ;; Server error response: {severity=ERROR, code=25006, file=utility.c, line=411, function=PreventCommandIfReadOnly, message=cannot execute DELETE in a read-only transaction, verbosity=ERROR}


  (pg/with-tx [conn {:rollback? true}]
    (pg/execute conn
                "delete from test1 where name like $1"
                {:params ["Test%"]})

    ;; pg14_1  | 2024-02-14 16:59:26.256 UTC [9020] LOG:  statement: BEGIN
    ;; pg14_1  | 2024-02-14 16:59:26.263 UTC [9020] LOG:  execute s11/p12: delete from test1 where name like $1
    ;; pg14_1  | 2024-02-14 16:59:26.263 UTC [9020] DETAIL:  parameters: $1 = 'Test%'
    ;; pg14_1  | 2024-02-14 16:59:26.267 UTC [9020] LOG:  statement: ROLLBACK


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

    (pg/execute conn
                "select pg_sleep($1) as sleep"
                {:params [1]})

    ;; LOG:  statement: BEGIN
    ;; LOG:  execute s3/p4: delete from demo where id = $1
    ;; DETAIL:  parameters: $1 = '3'
    ;; LOG:  execute s5/p6: insert into demo (title) values ($1)
    ;; DETAIL:  parameters: $1 = 'test4'
    ;; LOG:  statement: COMMIT

    ))
