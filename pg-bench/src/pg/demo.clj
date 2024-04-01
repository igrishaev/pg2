(ns pg.demo
  (:import
   clojure.lang.PersistentHashSet
   clojure.lang.Keyword
   java.util.UUID
   com.fasterxml.jackson.core.JsonGenerator
   com.fasterxml.jackson.databind.ObjectMapper)
  (:require
   [jsonista.core :as j]
   [jsonista.tagged :as jt]
   [pg.honey :as pgh]
   [pg.json :as json]
   [pg.oid :as oid]
   [pg.jdbc :as jdbc]
   [pg.pool :as pool]
   [pg.core :as pg]))


(def tagged-mapper
  (j/object-mapper
   {:encode-key-fn true
    :decode-key-fn true
    :modules
    [(jt/module
      {:handlers
       {Keyword {:tag "!kw"
                 :encode jt/encode-keyword
                 :decode keyword}
        PersistentHashSet {:tag "!set"
                           :encode jt/encode-collection
                           :decode set}}})]}))


(comment

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


  ;;
  ;; Arrays
  ;;

  ;; plain array: table, insert, select
  ;; 2d array: table, insert, select
  ;; 3d array: table, insert, select
  ;; array operators with params
  ;; UUIDs, Time, JSON. JSON.Wrapper case
  ;; oid hints

  (pg/query conn "create table add_demo_1 (id serial, text_arr text[])")

  (pg/execute conn
              "insert into add_demo_1 (text_arr) values ($1)"
              {:params [["one" "two" "three"]]})

  (pg/execute conn
              "insert into add_demo_1 (text_arr) values ($1)"
              {:params [["foo" nil "bar"]]})

  (pg/query conn "select * from add_demo_1")

  [{:id 1 :text_arr ["one" "two" "three"]}
   {:id 2 :text_arr ["foo" nil "bar"]}]

  (pg/execute conn
              "select * from add_demo_1 where text_arr && $1"
              {:params [["three" "four" "five"]]})

  [{:text_arr ["one" "two" "three"], :id 1}]

  (pg/execute conn
              "select * from add_demo_1 where text_arr @> $1"
              {:params [["foo" "bar"]]})


  [{:text_arr ["foo" nil "bar"], :id 2}]

  (pg/query conn "create table add_demo_2 (id serial, matrix bigint[][])")

  (pg/execute conn
              "insert into add_demo_2 (matrix) values ($1)"
              {:params [[[[1 2] [3 4] [5 6]]
                         [[6 5] [4 3] [2 1]]]]})

  {:inserted 1}

  (pg/query conn "select * from add_demo_2")

  [{:id 1 :matrix [[[1 2] [3 4] [5 6]]
                   [[6 5] [4 3] [2 1]]]}]










  ;;
  ;; JSON
  ;;

  (pg/query conn "create table test_json (id serial primary key, data jsonb not null)")

  (pg/execute conn
              "insert into test_json (data) values ($1)"
              {:params [{:some {:nested {:json 42}}}]})
  {:inserted 1}

  ;; insert into test_json (data) values ($1)
  ;; parameters: $1 = '{"some":{"nested":{"json":42}}}'

  (pg/execute conn
              "select * from test_json where id = $1"
              {:params [1]
               :first? true})

  {:id 1 :data {:some {:nested {:json 42}}}}

  (pgh/get-by-id conn :test_json 1)
  {:id 1, :data {:some {:nested {:json 42}}}}

  (pgh/insert-one conn
                  :test_json
                  {:data [:lift {:another {:json {:value [1 2 3]}}}]})

  {:id 2, :data {:another {:json {:value [1 2 3]}}}}

  (pgh/insert-one conn
                  :test_json
                  {:data [:param :data]}
                  {:honey {:params {:data {:some [:json {:map [1 2 3]}]}}}})


  (pg/execute conn
              "insert into test_json (data) values ($1)"
              {:params [[:some :vector [:nested :vector]]]})

  (pgh/get-by-id conn :test_json 3)
  {:id 3, :data ["some" "vector" ["nested" "vector"]]}

  (pgh/insert-one conn
                  :test_json
                  {:data (pg/json-wrap 42)})
  {:id 4, :data 42}

  (pgh/insert-one conn
                  :test_json
                  {:data (pg/json-wrap nil)})

  {:id 5, :data nil} ;; "null"



  (pg/execute conn "select * from test_json")

  (def -dump
    (json/write-string tagged-mapper #{:foo :bar :baz}))

  (println -dump)
  ["!set",[["!kw","baz"],["!kw","bar"],["!kw","foo"]]]

  (json/read-string tagged-mapper -dump)
  #{:baz :bar :foo}

  (def config
    {:host "127.0.0.1"
     :port 10140
     :user "test"
     :password "test"
     :dbname "test"
     :object-mapper tagged-mapper})

  (def conn
    (jdbc/get-connection config))

  (pg/execute conn
              "insert into test_json (data) values ($1) returning *"
              {:params [{:object #{:foo :bar :baz}}]})

  (pg/execute conn "select * from test_json")

  [{:id 1, :data {:object #{:baz :bar :foo}}}]

  (printl (pg/execute conn "select data::text json_raw from test_json where id = 10"))

  ;; [{:json_raw {"object": ["!set", [["!kw", "baz"], ["!kw", "bar"], ["!kw", "foo"]]]}}]

  ;; end json

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
  ;; Honey
  ;;

  (require '[pg.honey :as pgh])

  (def conn
    (pg/connect config))

  (pg/query conn "create table test003 (id integer not null, name text not null, active boolean not null default true)")

  (pg/query conn "insert into test003 (id, name, active) values (1, 'Ivan', true), (2, 'Huan', false), (3, 'Juan', true)")
  ;; {:inserted 3}

  (pgh/get-by-id conn :test003 1)
  ;; {:name "Ivan", :active true, :id 1}

  (pgh/get-by-id conn
                 :test003
                 1
                 {:pk [:raw "test003.id"]
                  :fields [:id :name]})

  ;; {:name "Ivan", :id 1}

  ;; SELECT id, name FROM test003 WHERE test003.id = $1 LIMIT $2
  ;; parameters: $1 = '1', $2 = '1'

  (pgh/get-by-ids conn :test003 [1 3 999])

  [{:name "Ivan", :active true, :id 1}
   {:name "Juan", :active true, :id 3}]

  (pgh/get-by-ids conn
                  :test003
                  [1 3 999]
                  {:pk [:raw "test003.id"]
                   :fields [:id :name]
                   :order-by [[:id :desc]]})

  [{:name "Juan", :id 3}
   {:name "Ivan", :id 1}]

  ;; SELECT id, name FROM test003 WHERE test003.id IN ($1, $2, $3) ORDER BY id DESC
  ;; parameters: $1 = '1', $2 = '3', $3 = '999'

  (pgh/delete conn
              :test003
              {:where [:and
                       [:= :id 3]
                       [:= :active true]]
               :returning [:*]})

  [{:name "Juan", :active true, :id 3}]

  (pgh/delete conn :test003)

  [{:name "Ivan", :active true, :id 1}
   {:name "Huan", :active false, :id 2}]


  (pg/query conn "create table test004 (id serial primary key, name text not null, active boolean not null default true)")

  (pgh/update conn
              :test003
              {:active true})

  [{:name "Ivan", :active true, :id 1}
   {:name "Huan", :active true, :id 2}
   {:name "Juan", :active true, :id 3}]

  (pgh/update conn
              :test003
              {:active false}
              {:where [:= :name "Ivan"]
               :returning [:id]})

  [{:id 1}]

  (pgh/update conn
              :test003
              {:id [:+ :id 100]
               :active [:not :active]
               :name [:raw "name || name"]}
              {:where [:= :name "Ivan"]
               :returning [:id :active]})

  [{:active true, :id 101}]

  ;; UPDATE test003
  ;;   SET
  ;;     id = id + $1,
  ;;     active = NOT active,
  ;;     name = name || name
  ;;   WHERE name = $2
  ;;   RETURNING id, active
  ;; parameters: $1 = '100', $2 = 'Ivan'

  (pgh/insert conn
              :test004
              [{:name "Foo" :active false}
               {:name "Bar" :active true}]
              {:returning [:id :name]})

  [{:name "Foo", :id 1}
   {:name "Bar", :id 2}]

  (pgh/insert conn
              :test004
              [{:id 1 :name "Snip"}
               {:id 2 :name "Snap"}]
              {:on-conflict [:id]
               :do-update-set [:name]
               :returning [:id :name]})

  ;; INSERT INTO test004 (id, name) VALUES ($1, $2), ($3, $4)
  ;;   ON CONFLICT (id)
  ;;   DO UPDATE SET name = EXCLUDED.name
  ;;   RETURNING id, name
  ;; parameters: $1 = '1', $2 = 'Snip', $3 = '2', $4 = 'Snap'

  (pg/query conn "select * from test004")

  [{:name "Snip", :active false, :id 1}
   {:name "Snap", :active true, :id 2}]

  (pgh/insert-one conn
                  :test004
                  {:id 2 :name "Alter Ego" :active true}
                  {:on-conflict [:id]
                   :do-update-set [:name :active]
                   :returning [:*]})

  {:name "Alter Ego", :active true, :id 2}

  ;; INSERT INTO test004 (id, name, active) VALUES ($1, $2, TRUE)
  ;;   ON CONFLICT (id)
  ;;   DO UPDATE SET name = EXCLUDED.name, active = EXCLUDED.active
  ;;   RETURNING *
  ;; parameters: $1 = '2', $2 = 'Alter Ego'

  (pgh/find conn :test003 {:active true})

  [{:name "Ivan", :active true, :id 1}
   {:name "Juan", :active true, :id 3}]

  (pgh/find conn :test003 {:active true
                           :name "Juan"})

  [{:name "Juan", :active true, :id 3}]

  (pgh/update conn
              :test003
              {:active true})

  ;; SELECT * FROM test003 WHERE (active = TRUE) AND (name = $1)
  ;; parameters: $1 = 'Juan'

  (pgh/find conn
            :test003
            {:active true}
            {:fields [:id :name]
             :limit 10
             :offset 1
             :order-by [[:id :desc]]
             :fn-key identity})

  [{"id" 1, "name" "Ivan"}]

  ;; SELECT id, name FROM test003
  ;;   WHERE (active = TRUE)
  ;;   ORDER BY id DESC
  ;;   LIMIT $1
  ;;   OFFSET $2
  ;; parameters: $1 = '10', $2 = '1'

  (pgh/find-first conn :test003
                  {:active true}
                  {:fields [:id :name]
                   :offset 1
                   :order-by [[:id :desc]]
                   :fn-key identity})

  {"id" 1, "name" "Ivan"}


  (def stmt
    (pgh/prepare conn {:select [:*]
                       :from :test003
                       :where [:= :id 0]}))

  ;; <Prepared statement, name: s37, param(s): 1, OIDs: [INT8], SQL: SELECT * FROM test003 WHERE id = $1>

  (pg/execute-statement conn stmt {:params [3]
                                   :first? true})

  {:name "Juan", :active true, :id 3}

  (def stmt
    (pgh/prepare conn {:select [:*]
                       :from :test003
                       :where [:raw "id = $1"]}))

  (pg/execute-statement conn stmt {:params [1]
                                   :first? true})

  {:name "Ivan", :active true, :id 1}

  (pgh/query conn
             {:select [:id]
              :from :test003
              :order-by [:id]})

  ;; [{:id 1} {:id 2} {:id 3}]

  (pgh/query conn
             {:select [:id]
              :from :test003
              :where [:= :name "Ivan"]
              :order-by [:id]})

  ;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:207).
  ;; Server error response: {severity=ERROR, code=42P02, file=parse_expr.c, line=842, function=transformParamRef, position=37, message=there is no parameter $1, verbosity=ERROR}

  (pgh/query conn
             {:select [:id]
              :from :test003
              :where [:raw "name = 'Ivan'"]
              :order-by [:id]})

  [{:id 1}]

  ;; SELECT id FROM test003 WHERE name = 'Ivan' ORDER BY id ASC

  (pgh/execute conn
               {:select [:id]
                :from :test003
                :where [:= :name "Ivan"]
                :order-by [:id]})

  [{:id 1}]

  (pgh/query conn
             {:select [:id]
              :from :test003
              :where [:= :name "Ivan"]
              :order-by [:id]}
             {:honey {:inline true}})

  (pgh/execute conn
               {:select [:id :name]
                :from :test003
                :where [:= :name "Ivan"]
                :order-by [:id]})

  [{:name "Ivan", :id 1}]


  (pgh/execute conn
               {:select [:id :name]
                :from :test003
                :where [:= :name [:param :name]]
                :order-by [:id]}
               {:honey {
                        :params {:name "Ivan"}}})

  [{:name "Ivan", :id 1}]

  ;; SELECT id, name FROM test003 WHERE name = $1 ORDER BY id ASC
  ;; parameters: $1 = 'Ivan'




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
