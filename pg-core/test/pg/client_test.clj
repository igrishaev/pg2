(ns pg.client-test
  (:import
   java.io.ByteArrayOutputStream
   java.io.File
   java.io.InputStream
   java.io.OutputStream
   java.time.Instant
   java.time.LocalDate
   java.time.LocalDateTime
   java.time.LocalTime
   java.time.OffsetDateTime
   java.time.OffsetTime
   java.util.ArrayList
   java.util.Date
   java.util.HashMap
   java.util.concurrent.ExecutionException
   org.pg.clojure.RowMap
   org.pg.error.PGError
   org.pg.error.PGErrorResponse)
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [com.stuartsierra.component :as component]
   [jsonista.core :as j]
   [less.awful.ssl :as ssl]
   [pg.component :as pgc]
   [pg.core :as pg]
   [pg.fold :as fold]
   [pg.honey :as pgh]
   [pg.integration :refer [*CONFIG-TXT*
                           *CONFIG-BIN*
                           *PORT*
                           fix-multi-port]]
   [pg.json :as json]
   [pg.oid :as oid]
   [pg.pool :as pool]))


(use-fixtures :each fix-multi-port)


(defn reverse-string [s]
  (apply str (reverse s)))


(def custom-mapper
  (j/object-mapper
   {:encode-key-fn (comp reverse-string name)
    :decode-key-fn (comp keyword reverse-string)}))


(defn gen-table []
  (format "table_%s" (System/nanoTime)))


(defn gen-type []
  (format "type_%s" (System/nanoTime)))


(deftest test-client-tx-status

  (pg/with-connection [conn *CONFIG-TXT*]

    (is (= :I (pg/status conn)))

    (is (pg/idle? conn))

    (pg/query conn "select 1")

    (is (= :I (pg/status conn)))

    (pg/begin conn)

    (is (= :T (pg/status conn)))

    (is (pg/in-transaction? conn))

    (try
      (pg/query conn "selekt 1")
      (is false)
      (catch PGErrorResponse _
        (is true)))

    (is (= :E (pg/status conn)))

    (is (pg/tx-error? conn))

    (pg/rollback conn)

    (is (= :I (pg/status conn)))))


(def QUERY_SELECT_RANDOM_COMPLEX
  "
select
  x::int4                  as int4,
  x::int8                  as int8,
  x::numeric               as numeric,
  x::text || 'foobar'      as line,
  x > 100500               as bool,
  now()                    as ts,
  now()::date              as date,
  now()::time              as time,
  '2024-01-13 21:08:57.593323+05:30'::timestamptz,
  '2024-01-13 21:08:57.593323+05:30'::timestamp,
  null                     as nil
from
  generate_series(1,2) as s(x)
")


(deftest test-client-complex-txt
  (pg/with-conn [conn *CONFIG-TXT*]
    (let [res
          (pg/query conn QUERY_SELECT_RANDOM_COMPLEX)]
      (is true))))


(deftest test-client-complex-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/query conn QUERY_SELECT_RANDOM_COMPLEX)]
      (is true))))


(deftest test-client-conn-str-print
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [repr
          (format "<PG connection test@127.0.0.1:%s/test>" *PORT*)]
      (is (= repr (str conn)))
      (is (= repr (with-out-str
                    (print conn)))))))


(deftest test-client-conn-equals
  (pg/with-connection [conn1 *CONFIG-TXT*]
    (pg/with-connection [conn2 *CONFIG-TXT*]
      (is (= conn1 conn1))
      (is (not= conn1 conn2)))))


(deftest test-client-ok
  (let [result
        (pg/with-connection [conn *CONFIG-TXT*]
          (pg/execute conn "select 1 as foo, 'hello' as bar"))]
    (is (= [{:foo 1 :bar "hello"}]
           result))))


(deftest test-client-query-multiple

  (let [result
        (pg/with-connection [conn *CONFIG-TXT*]
          (pg/query conn "select 1 as foo; select 'two' as bar"))]

    (is (= [[{:foo 1}]
            [{:bar "two"}]]
           result))))


(deftest test-client-empty-query

  (let [result
        (pg/with-connection [conn *CONFIG-TXT*]
          (pg/query conn ""))]

    (is (nil? result))))


(deftest test-client-fn-column
  (let [result
        (pg/with-connection [conn *CONFIG-TXT*]
          (pg/execute conn "select 1 as foo" {:fn-key str/upper-case}))]
    (is (= [{"FOO" 1}] result))))


(deftest test-client-fn-column-kebab
  (let [result
        (pg/with-connection [conn *CONFIG-TXT*]
          (pg/execute conn "select 1 as just_one" {:kebab-keys? true}))]
    (is (= [{:just-one 1}] result))))


(deftest test-client-read-only-on

  (let [res
        (pg/with-connection [conn (assoc *CONFIG-TXT* :read-only? true)]
          (pg/query conn "show default_transaction_read_only"))]
    (is (= [{:default_transaction_read_only "on"}] res)))

  (pg/with-connection [conn (assoc *CONFIG-TXT* :read-only? true)]
    (try
      (pg/query conn "create table foo(id serial)")
      (is false)
      (catch PGErrorResponse e
        (is (-> e
                (ex-message)
                (str/includes? "cannot execute CREATE TABLE in a read-only transaction"))))))

  (let [res
        (pg/with-connection [conn (assoc *CONFIG-TXT* :read-only? true)]
          (pg/query conn "show transaction_read_only"))]
    (is (= [{:transaction_read_only "on"}] res)))

  (pg/with-connection [conn (assoc *CONFIG-TXT* :read-only? true)]
    (pg/with-tx [conn {:read-only? false}]
      (try
        (pg/query conn "create table foo(id serial)")
        (is false)
        (catch PGErrorResponse e
          (is (-> e
                  (ex-message)
                  (str/includes? "cannot execute CREATE TABLE in a read-only transaction"))))))))


(deftest test-client-keyword-with-ns
  (let [result
        (pg/with-connection [conn *CONFIG-TXT*]
          (pg/execute conn "select 1 as \"user/foo-bar\""))]
    (is (= [{:user/foo-bar 1}] result))))


(deftest test-client-exception-in-the-middle
  (pg/with-connection [conn *CONFIG-TXT*]
    (try
      (pg/execute conn "select $1 as foo" {:params [(new Object)]})
      (is false)
      (catch PGError e
        (is true)
        (is (-> e
                ex-message
                (str/starts-with? "cannot text-encode a value")))))

    (testing "still can use that connection"
      (is (= [{:foo "test"}]
             (pg/execute conn "select $1 as foo" {:params ["test"]}))))))


(deftest test-client-reuse-conn
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res1
          (pg/query conn "select 1 as foo")
          res2
          (pg/query conn "select 'hello' as bar")]
      (is (= [{:foo 1}] res1))
      (is (= [{:bar "hello"}] res2)))))


(deftest test-client-lazy-map
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [[row1]
          (pg/query conn "select 42 as foo, 'test' as bar")]

      (is (map? row1))
      (is (instance? RowMap row1))

      (is (= [:bar :foo] (sort (keys row1))))
      (is (= #{42 "test"} (set (vals row1))))

      (is (= row1 {:foo 42 :bar "test"}))
      (is (= {:foo 42 :bar "test"} row1))

      (is (= {:foo 42 :x 3}
             (-> row1
                 (assoc :x 3)
                 (dissoc :bar))))

      (is (= {} (empty row1)))

      (is (nil? (get row1 :aaa)))
      (is (= :bbb (get row1 :aaa :bbb)))

      (is (= #{[:bar "test"] [:foo 42]} (-> row1 seq set)))
      (is (= 2 (count row1))))))


(deftest test-client-vector
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [[_ row1]
          (pg/query conn
                    "select 42 as foo, 'test' as bar, 43 as foo"
                    {:table true})]

      (is (= [42 "test" 43] row1))
      (is (= row1 [42 "test" 43]))

      (is (vector? row1))
      (is (= 3 (count row1)))

      (is (nil? (get row1 -1)))
      (is (nil? (get row1 99)))

      (is (= [42 "test" 43 3] (conj row1 3)))

      (is (= [] (empty row1)))

      (is (= [42 "test"] (pop row1)))
      (is (= 43 (peek row1)))
      (is (= (list 42 "test" 43) (seq row1))))))


(deftest test-client-socket-opt

  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :so-keep-alive? true
                                   :so-tcp-no-delay? true
                                   :so-timeout 999
                                   :so-recv-buf-size 123
                                   :so-send-buf-size 456)]

    (let [res1
          (pg/query conn "select 1 as foo")]

      (is (= [{:foo 1}] res1)))))


(deftest test-client-65k-params-execute

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [ids
          (range 1 (inc 0xFFFF))

          params
          (for [id ids]
            (format "$%d" id))

          q-marks
          (str/join "," params)

          query
          (format "select 65535 in (%s) answer" q-marks)

          res1
          (pg/execute conn query {:params ids})]

      (is (= [{:answer true}] res1)))))


(deftest test-client-with-tx-check
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-tx [conn]
      (is (pg/connection? conn)))))


(deftest test-client-with-transaction-ok

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [res1
          (pg/with-tx [conn]
            (pg/execute conn "select 1 as foo"))

          res2
          (pg/with-tx [conn]
            (pg/execute conn "select 2 as bar"))]

      (is (= [{:foo 1}] res1))
      (is (= [{:bar 2}] res2)))))


(deftest test-client-with-transaction-read-only

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [res1
          (pg/with-tx [conn {:read-only? true}]
            (pg/execute conn "select 1 as foo"))]

      (is (= [{:foo 1}] res1))

      (try
        (pg/with-tx [conn {:read-only? true}]
          (pg/execute conn "create temp table foo123 (id integer)"))
        (is false "Must have been an error")
        (catch PGErrorResponse e
          (is (-> e ex-message (str/includes? "cannot execute CREATE TABLE in a read-only transaction"))))))))


(deftest test-client-bpchar-txt
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-tx [conn]
      (pg/execute conn "create temp table foo123 (data bpchar)")
      (pg/execute conn "insert into foo123 values ('test')")
      (testing "decode txt"
        (let [res
              (pg/execute conn "select * from foo123")]
          (is (= [{:data "test"}] res)))))))


(deftest test-client-bpchar-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (pg/with-tx [conn]
      (pg/execute conn "create temp table foo123 (data bpchar)")
      (pg/execute conn "insert into foo123 values ('test')")
      (testing "decode txt"
        (let [res
              (pg/execute conn "select * from foo123")]
          (is (= [{:data "test"}] res)))))))


(deftest test-exeplain-analyze

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [result
          (pg/execute conn "explain analyze select 42")

          lines
          (mapv (keyword "QUERY PLAN") result)

          prefixes
          (for [line lines]
            (-> line (str/split #"\s") first))]

      (is (= ["Result" "Planning" "Execution"]
             prefixes)))))


(deftest test-client-with-transaction-iso-level

  (let [table
        (gen-table)]

    (pg/with-connection [conn *CONFIG-TXT*]

      (pg/execute conn (format "create table %s (id integer)" table))

      (pg/with-tx [conn {:isolation-level "SERIALIZABLE"}]
        (pg/execute conn (format "insert into %s values (1), (2)" table))

        (let [res1
              (pg/execute conn (format "select * from %s" table))

              res2
              (pg/with-connection [conn2 *CONFIG-TXT*]
                (pg/execute conn2 (format "select * from %s" table)))]

          (is (= [{:id 1} {:id 2}] res1))
          (is (= [] res2)))))))


(deftest test-client-with-transaction-complex
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-tx [conn {:isolation-level "SERIALIZABLE"
                       :read-only? true
                       :rollback? true}]
      (let [res (pg/execute conn "select 42 as number")]
        (is (= [{:number 42}] res))))))


(deftest test-client-enum-type
  (let [table
        (gen-table)

        type-name
        (gen-type)]

    (pg/with-connection [conn *CONFIG-BIN*]
      (pg/execute conn (format "create type %s as enum ('foo', 'bar', 'kek', 'lol')" type-name))
      (pg/execute conn (format "create table %s (id integer, foo %s)" table type-name))
      (pg/execute conn (format "insert into %s values (1, 'foo'), (2, 'bar')" table))
      (let [res1
            (pg/execute conn (format "select * from %s" table))]
        (is (= [{:foo "foo", :id 1} {:foo "bar", :id 2}]
               res1)))

      (pg/execute conn
                  (format "insert into %s (id, foo) values ($1, $2)" table)
                  {:params [3 "kek"]
                   :oids [oid/int8 oid/default]})

      (pg/execute conn
                  (format "insert into %s (id, foo) values ($1, $2)" table)
                  {:params [4 (pg/->enum :lol)]})

      (let [res1
            (pg/execute conn (format "select * from %s" table))]
        (is (= [{:foo "foo" :id 1}
                {:foo "bar" :id 2}
                {:foo "kek" :id 3}
                {:foo "lol" :id 4}]
               res1))))))


(deftest test-client-with-transaction-rollback

  (let [table
        (gen-table)]

    (pg/with-connection [conn *CONFIG-TXT*]

      (pg/execute conn (format "create table %s (id integer)" table))

      (pg/with-tx [conn {:rollback? true}]
        (pg/execute conn (format "insert into %s values (1), (2)" table)))

      (let [res1
            (pg/execute conn (format "select * from %s" table))]

        (is (= [] res1))))))


(deftest test-client-create-table
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query
          (format "create temp table %s (id serial, title text)" table)

          res
          (pg/execute conn query)]

      (is (= {:command "CREATE TABLE"}
             res)))))


(deftest test-client-listen-notify

  (let [capture!
        (atom [])

        fn-notification
        (fn [msg]
          (swap! capture! conj msg))

        config+
        (assoc *CONFIG-BIN* :fn-notification fn-notification)]

    (pg/with-connection [conn config+]

      (let [pid
            (pg/pid conn)

            channel
            "!@#$%^&*();\" d'rop \"t'a'ble students--;42"

            res1
            (pg/listen conn channel)

            message
            "'; \n\t\rdrop table studets--!@#$%^\""

            res2
            (pg/notify conn channel message)

            res3
            (pg/unlisten conn channel)

            res4
            (pg/notify conn channel "more")

            invocations
            @capture!]

        (is (nil? res1))
        (is (nil? res2))
        (is (nil? res3))
        (is (nil? res4))

        (is (= 1 (count invocations)))

        (is (= {:msg :NoticeResponse
                :pid pid
                :channel channel
                :message message}
               (first invocations)))))))


(deftest test-client-listen-notify-exception

  (let [capture!
        (atom [])

        fn-notification
        (fn [msg]
          (try
            (/ 0 0)
            (swap! capture! conj msg)
            (catch Throwable e
              (swap! capture! conj e))))

        config+
        (assoc *CONFIG-TXT* :fn-notification fn-notification)]

    (pg/with-connection [conn config+]

      (let [pid
            (pg/pid conn)

            channel
            "!@#$%^&*();\" d'rop \"t'a'ble students--;42"

            res1
            (pg/listen conn channel)

            message
            "'; \n\t\rdrop table studets--!@#$%^\""

            res2
            (pg/notify conn channel message)

            res3
            (pg/unlisten conn channel)

            res4
            (pg/notify conn channel "more")

            e
            (-> capture! deref first)]

        (is (nil? res1))
        (is (nil? res2))
        (is (nil? res3))
        (is (nil? res4))

        (is (instance? ArithmeticException e))
        (is (= "Divide by zero" (ex-message e)))))))


(deftest test-client-listen-notify-different-conns

  (let [capture!
        (atom [])

        fn-notification
        (fn [msg]
          (swap! capture! conj msg))

        config+
        (assoc *CONFIG-TXT* :fn-notification fn-notification)]

    (pg/with-connection [conn1 *CONFIG-TXT*]
      (pg/with-connection [conn2 config+]

        (let [pid1 (pg/pid conn1)
              pid2 (pg/pid conn2)]

          (pg/execute conn2 "listen FOO")
          (pg/execute conn1 "notify FOO, 'message1'")
          (pg/execute conn1 "notify FOO, 'message2'")

          (pg/execute conn2 "")

          (is (= (set [{:msg :NoticeResponse
                         :pid pid1
                         :channel "foo"
                         :message "message1"}

                        {:msg :NoticeResponse
                         :pid pid1
                         :channel "foo"
                         :message "message2"}])

                 (set @capture!))))))))


(deftest test-client-broken-query
  (pg/with-connection [conn *CONFIG-TXT*]
    (try
      (pg/execute conn "selekt 1")
      (is false "must have been an error")
      (catch PGErrorResponse e
        (is true)
        (is (-> e ex-message (str/includes? "syntax error")))))
    (testing "still can recover"
      (is (= [{:one 1}]
             (pg/execute conn "select 1 as one"))))))


(deftest test-client-error-response
  (let [config
        (assoc *CONFIG-TXT* :pg-params {"pg_foobar" "111"})]
    (try
      (pg/with-connection [conn config]
        42)
      (is false)
      (catch PGErrorResponse e
        (is true)
        (is (-> e ex-message (str/includes? "unrecognized configuration parameter")))))))


(deftest test-pg-error-response-fields
  (let [config
        (assoc *CONFIG-TXT* :pg-params {"pg_foobar" "111"})]
    (try
      (pg/with-connection [conn config]
        42)
      (is false)
      (catch PGErrorResponse e
        (let [fields
              (pg/get-error-fields e)]
          (is (= {:verbosity "FATAL"
                  :severity "FATAL"
                  :code "42704"
                  :message "unrecognized configuration parameter \"pg_foobar\""}
                 (dissoc fields
                         :file
                         :line
                         :function))))))))


(deftest test-client-wrong-startup-params

  (let [config
        (assoc *CONFIG-TXT* :pg-params {"application_name" "Clojure"
                                    "DateStyle" "ISO, MDY"})]

    (pg/with-connection [conn config]
      (let [param
            (pg/get-parameter conn "application_name")]
        (is (= "Clojure" param))))))


(deftest test-terminate-closed
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/close conn)
    (is (pg/closed? conn))))


(deftest test-client-prepare

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query1
          "prepare foo as select $1::integer as num"

          res1
          (pg/execute conn query1)

          query2
          "execute foo(42)"

          res2
          (pg/execute conn query2)

          query3
          "deallocate foo"

          res3
          (pg/execute conn query3)]

      (is (= {:command "PREPARE"} res1))
      (is (= [{:num 42}] res2))
      (is (= {:command "DEALLOCATE"} res3)))))


(deftest test-client-cursor

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          _
          (pg/execute conn query2)

          query3
          (format "DECLARE cur CURSOR for select * from %s" table)]

      (pg/with-tx [conn]

        (let [res3
              (pg/execute conn query3)

              res4
              (pg/execute conn "fetch next from cur")

              res5
              (pg/execute conn "fetch next from cur")

              res6
              (pg/execute conn "fetch next from cur")]

          (pg/execute conn "close cur")

          (is (= {:command "DECLARE CURSOR"} res3))

          (is (= [{:id 1 :title "test1"}] res4))
          (is (= [{:id 2 :title "test2"}] res5))
          (is (= [] res6)))))))


(deftest test-client-wrong-minor-protocol

  (let [capture!
        (atom [])

        config
        (assoc *CONFIG-TXT*
               :protocol-version 196609
               :fn-protocol-version
               (fn [msg]
                 (swap! capture! conj msg)))]

    (pg/with-connection [conn config]
      (is (= [{:foo 1}]
             (pg/execute conn "select 1 as foo"))))

    (is (= {:msg :NegotiateProtocolVersion
            :params []
            :param-count 0
            :version 196608}
           (-> capture! deref first)))))


(deftest test-client-wrong-major-protocol
  (let [config
        (assoc *CONFIG-TXT* :protocol-version 296608)]
    (try
      (pg/with-connection [conn config]
        (pg/execute conn "select 1 as foo"))
      (is false)
      (catch PGErrorResponse e
        (is true)
        (is (-> e ex-message (str/includes? "unsupported frontend protocol")))))))


(deftest test-client-empty-select
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "select * from %s" table)

          res
          (pg/execute conn query2)]

      (is (= [] res)))))


(deftest test-client-insert-result-returning
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2') returning *" table)

          res
          (pg/execute conn query2)]

      (is (= [{:id 1 :title "test1"}
              {:id 2 :title "test2"}]
             res)))))


(deftest test-client-notice-custom-function
  (let [capture!
        (atom nil)

        config
        (assoc *CONFIG-TXT* :fn-notice
               (fn [message]
                 (reset! capture! message)))]

    (pg/with-connection [conn config]
      (let [res (pg/execute conn "ROLLBACK")]
        (is (= {:command "ROLLBACK"} res))))

    (Thread/sleep 100)

    (is (= {:msg :NoticeResponse
            :verbosity "WARNING",
            :function "UserAbortTransactionBlock",
            :severity "WARNING",
            :code "25P01",
            :message "there is no transaction in progress"}
           (-> capture!
               (deref)
               (dissoc :line :file))))))


(deftest test-client-insert-result-no-returning
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          res
          (pg/execute conn query2)]

      (is (= {:inserted 2} res)))))


(deftest test-client-select-fn-first
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (pg/execute conn query2)

          query3
          (format "select * from %s where id = 1" table)

          res
          (pg/execute conn query3 {:first true})]

      (is (= {:id 1 :title "test1"} res)))))


(deftest test-prepare-result
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/prepare conn "select $1::integer as foo")]
      (is (pg/prepared-statement? res)))))


(deftest test-prepare-with-oids
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [stmt
          (pg/prepare conn
                      "select $1 as foo"
                      {:oids [oid/int4]})
          res
          (pg/execute-statement conn
                                stmt
                                {:params [999]
                                 :first true})]
      (is (= {:foo 999} res)))))


(deftest test-statement-params-wrong-count
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-statement [stmt conn "select $1::integer as foo, $2::integer as bar"]
      (try
        (pg/execute-statement conn stmt {:params [1]})
        (is false)
        (catch PGError e
          (is (= "Wrong parameters count: 1 (must be 2)"
                 (ex-message e))))))))


(deftest test-statement-params-nil
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-statement [stmt conn "select 42 as answer"]
      (let [res (pg/execute-statement conn stmt {:params nil})]
        (is (= [{:answer 42}] res))))))


(deftest test-prepare-execute

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/with-statement [stmt conn "select $1::integer as foo"]

      (let [res1
            (pg/execute-statement conn stmt {:params [1]})

            res2
            (pg/execute-statement conn stmt {:params [2]})]

        (is (= [{:foo 1}] res1))
        (is (= [{:foo 2}] res2))))))


(deftest test-prepare-execute-with-options

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/with-statement [stmt conn "select $1::integer as foo"]

      (let [res1
            (pg/execute-statement conn stmt {:params [1]
                                             :fn-key str/upper-case})

            res2
            (pg/execute-statement conn stmt {:params [2]
                                             :first true})]

        (is (= [{"FOO" 1}] res1))
        (is (= {:foo 2} res2))))))


(deftest test-client-delete-result
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (pg/execute conn query2)

          query3
          (format "delete from %s " table)

          res
          (pg/execute conn query3)]

      (is (= {:deleted 2} res)))))


(deftest test-client-update-result
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (pg/execute conn query2)

          query3
          (format "update %s set title = 'aaa'" table)

          res
          (pg/execute conn query3)]

      (is (= {:updated 2} res)))))


(deftest test-client-mixed-result
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query
          (format
           "
create temp table %1$s (id serial, title text);
insert into %1$s (id, title) values (1, 'test1'), (2, 'test2');
insert into %1$s (id, title) values (3, 'test3') returning *;
select * from %1$s where id <> 3;
update %1$s set title = 'aaa' where id = 1;
delete from %1$s where id = 2;
drop table %1$s;
"
           table)

          res
          (pg/query conn query {:fn-key str/upper-case})]

      (is (= [{:command "CREATE TABLE"}
              {:inserted 2}
              [{"TITLE" "test3", "ID" 3}]
              [{"TITLE" "test1", "ID" 1}
               {"TITLE" "test2", "ID" 2}]
              {:updated 1}
              {:deleted 1}
              {:command "DROP TABLE"}]
             res)))))


(deftest test-client-truncate-result
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query1
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query1)

          query2
          (format "insert into %s (id, title) values (1, 'test1'), (2, 'test2')" table)

          _
          (pg/execute conn query2)

          query3
          (format "truncate %s" table)

          res
          (pg/execute conn query3)]

      (is (= {:command "TRUNCATE TABLE"} res)))))


(deftest test-client-select-multi
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/query conn "select 1 as foo; select 2 as bar")]
      (is (= [[{:foo 1}] [{:bar 2}]] res)))))


(deftest test-client-field-duplicates
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn "select 1 as id, 2 as id")]
      (is (= [{:id 1 :id_1 2}] res)))))


(deftest test-client-insert-simple
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [table
          (gen-table)

          command
          (format "create table %s (id integer, title text)" table)

          _
          (pg/execute conn command)

          res
          (pg/execute conn
                      (format "insert into %s (id, title) values ($1, $2), ($3, $4) returning *" table)
                      {:params [1 "test1" 2 "test2"]})]

      (is (= [{:title "test1", :id 1}
              {:title "test2", :id 2}]
             res)))))


(deftest test-client-json-read
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn "select '[1, 2, 3]'::json as arr")]
      (is (= [{:arr [1 2 3]}] res)))))


(deftest test-client-jsonb-read
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn "select '{\"foo\": 123}'::jsonb as obj")]
      (is (= [{:obj {:foo 123}}] res)))))


(deftest test-client-json-wrapper
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn
                      "select $1::json as obj"
                      {:params [(pg/json-wrap 42)]
                       :first true})]
      (is (= {:obj 42} res)))))


(deftest test-client-json-wrapper-nil
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn
                      "select $1::json as obj"
                      {:params [(pg/json-wrap nil)]
                       :first true})]
      (is (= {:obj nil} res)))))


(deftest test-client-json-write
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn
                      "select $1::json as obj"
                      {:params [{:foo 123}]
                       :first true})]
      (is (= {:obj {:foo 123}} res)))))


(deftest test-client-jsonb-parsing-version

  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn
                      "select '[1, 2, 3]'::json as obj")]
      (is (= [{:obj [1 2 3]}] res))))

  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn
                      "select '[1, 2, 3]'::jsonb as obj")]
      (is (= [{:obj [1 2 3]}] res))))

  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn
                      "select '[1, 2, 3]'::jsonb as obj")]
      (is (= [{:obj [1 2 3]}] res))))

  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn
                      "select $1::jsonb as obj"
                      {:params ["[1, 2, 3]"]})]
      (is (= [{:obj [1 2 3]}] res)))))


(deftest test-client-json-write-no-hint
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn
                      "select $1::json as obj"
                      {:params [{:foo 123}]
                       :first true})]
      (is (= {:obj {:foo 123}} res)))))


(deftest test-client-json-write-oid-hint
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn
                      "select $1 as obj"
                      {:params [{:foo 123}]
                       :oids [oid/jsonb]
                       :first true})]
      (is (= {:obj {:foo 123}} res)))))


(deftest test-client-jsonb-write
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [json
          [1 2 [true {:foo 1}]]

          res
          (pg/execute conn
                      "select $1::jsonb as obj"
                      {:params [json]
                       :first first})]
      (is (= '{:obj (1 2 [true {:foo 1}])} res)))))


(deftest test-client-json-read-txt-object-mapper
  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? false
                                   :binary-decode? false
                                   :object-mapper custom-mapper)]
    (let [res
          (pg/execute conn "select '{\"foo\": 123}'::jsonb as obj")]
      (is (= [{:obj {:oof 123}}] res)))))


(deftest test-client-json-read-bin-object-mapper
  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? true
                                   :binary-decode? true
                                   :object-mapper custom-mapper)]
    (let [res
          (pg/execute conn "select '{\"foo\": 123}'::jsonb as obj")]
      (is (= [{:obj {:oof 123}}] res)))))


(deftest test-client-json-write-txt-object-mapper
  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? false
                                   :binary-decode? false
                                   :object-mapper custom-mapper)]
    (pg/execute conn "create temp table foo123 (val json)")
    (pg/execute conn "insert into foo123 values ($1)" {:params [{:foo 123}]})
    (let [res
          (pg/execute conn "select val::text from foo123")]
      (is (= [{:val "{\"oof\":123}"}] res)))))


(deftest test-client-json-write-bin-object-mapper
  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? true
                                   :binary-decode? true
                                   :object-mapper custom-mapper)]
    (pg/execute conn "create temp table foo123 (val json)")
    (pg/execute conn "insert into foo123 values ($1)" {:params [{:foo 123}]})
    (let [res
          (pg/execute conn "select val::text from foo123")]
      (is (= [{:val "{\"oof\":123}"}] res)))))


(deftest test-client-default-oid-long
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select $1::int8 as foo" {:params [42]})]
      (is (= [{:foo 42}] res)))))


(deftest test-client-default-oid-uuid
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [uid (random-uuid)
          res (pg/execute conn "select $1::uuid as foo" {:params [uid]})]
      (is (= [{:foo uid}] res)))))


(deftest test-client-execute-sqlvec
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select $1 as foo" {:params ["hi"]})]
      (is (= [{:foo "hi"}] res)))))


(deftest test-client-execute-sqlvec-no-params
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select 42 as foo")]
      (is (= [{:foo 42}] res)))))


(deftest test-client-timestamptz-read
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select '2022-01-01 23:59:59.123+03'::timestamptz as obj")
          obj (-> res first :obj)]
      (is (instance? OffsetDateTime obj))
      (is (= "2022-01-01T20:59:59.123Z" (str obj))))))


(deftest test-client-timestamptz-pass
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-statement [stmt conn "select $1::timestamptz as obj"]
      (let [inst
            (Instant/parse "2022-01-01T20:59:59.123456Z")
            res
            (pg/execute-statement conn stmt {:params [inst]})
            obj
            (-> res first :obj)]
        (is (instance? OffsetDateTime obj))
        (is (= (str inst) (str obj)))))))


(deftest test-client-timestamp-read
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select '2022-01-01 23:59:59.123+03'::timestamp as obj")
          obj (-> res first :obj)]
      (is (instance? LocalDateTime obj))
      (is (= "2022-01-01T23:59:59.123" (str obj))))))


(deftest test-client-timestamp-pass
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-statement [stmt conn "select $1::timestamp as obj"]
      (let [inst
            (Instant/parse "2022-01-01T20:59:59.000000123Z")
            res
            (pg/execute-statement conn stmt {:params [inst]})]

        (is (= "2022-01-01T20:59:59"
               (-> res first :obj str)))))))


(deftest test-client-instant-date-read
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select '2022-01-01 23:59:59.123+03'::date as obj")
          obj (-> res first :obj)]
      (is (instance? LocalDate obj))
      (is (= "2022-01-01" (str obj))))))


(deftest test-client-pass-date-timestamptz
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [date
          (new Date 85 11 31 23 59 59)

          res
          (pg/execute conn "select $1::timestamptz as obj" {:params [date]})

          obj
          (-> res first :obj)]

      (is (instance? OffsetDateTime obj))
      (is (= "1985-12-31T20:59:59Z" (str obj))))))


(deftest test-client-date-pass-date

  (pg/with-connection [conn *CONFIG-TXT*]
    (let [date
          (new Date 85 11 31 23 59 59)

          res
          (pg/execute conn "select EXTRACT('year' from $1::date) as year" {:params [date]})]

      (is (= (update-in res [0 :year] int)
             [{:year 1985}])))))


(deftest test-client-read-time

  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn "select now()::time as time")

          time
          (-> res first :time)]

      (is (instance? LocalTime time)))))


(deftest test-client-pass-time

  (pg/with-connection [conn *CONFIG-TXT*]
    (let [time1
          (LocalTime/now)

          res
          (pg/execute conn "select $1::time as time" {:params [time1]})

          time2
          (-> res first :time)]

      (is (= time1 time2)))))


(deftest test-client-read-timetz

  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn "select now()::timetz as timetz")

          timetz
          (-> res first :timetz)]

      (is (instance? OffsetTime timetz)))))


(deftest test-client-pass-timetz

  (pg/with-connection [conn *CONFIG-TXT*]
    (let [time1
          (OffsetTime/now)

          res
          (pg/execute conn "select $1::timetz as timetz" {:params [time1]})

          time2
          (-> res first :timetz)]

      (is (= time1 time2)))))


(deftest test-client-conn-with-open
  (with-open [conn (pg/connect *CONFIG-TXT*)]
    (let [res (pg/execute conn "select 1 as one")]
      (is (= [{:one 1}] res)))))


(deftest test-client-prepare-&-close-ok

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [stmt
          (pg/prepare conn "select 1 as foo")]

      (is (pg/prepared-statement? stmt))

      (let [result
            (pg/close-statement conn stmt)]

        (is (nil? result))

        (try
          (pg/execute-statement conn stmt)
          (is false)
          (catch PGErrorResponse e
            (is true)
            (is (-> e ex-message (str/includes? "does not exist")))))))))


(deftest test-execute-row-limit
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo as (values (1, 2), (3, 4), (5, 6)) select * from foo"]

      (pg/with-statement [stmt conn query]

        (let [result
              (pg/execute-statement conn stmt {:max-rows 1})]

          (is (= [{:column1 1 :column2 2}]
                 result)))))))


(deftest test-execute-recover-from-exception
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo as (values (1, 2), (3, 4), (5, 6)) select * from foo"]

      (pg/with-statement [stmt conn query]

        (try
          (pg/execute-statement conn stmt
                                {:run (fn [_]
                                        (/ 0 0))})
          (is false)
          (catch Throwable e
            (is (= "Unhandled exception: Divide by zero" (-> e ex-message)))
            (is (= "Divide by zero" (-> e ex-cause ex-message)))))

        (testing "conn is usable"
          (is (= :I (pg/status conn)))
          (is (= [{:one 1}] (pg/query conn "select 1 as one"))))))))


(deftest test-execute-row-limit-int32-unsigned
  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo as (values (1, 2), (3, 4), (5, 6)) select * from foo"]

      (pg/with-statement [stmt conn query]

        (let [result
              (pg/execute-statement conn stmt {:max-rows 0xFFFFFFFF})]

          (is (= [{:column1 1 :column2 2}
                  {:column1 3 :column2 4}
                  {:column1 5 :column2 6}]
                 result)))))))


(deftest test-acc-as-java

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res1
          (pg/execute conn query {:as fold/java
                                  :fn-key identity})

          res2
          (pg/execute conn query {:java true
                                  :fn-key identity})]

      (is (= [{"b" 2 "a" 1}
              {"b" 4 "a" 3}
              {"b" 6 "a" 5}]
             res1))

      (is (= [{"b" 2 "a" 1}
              {"b" 4 "a" 3}
              {"b" 6 "a" 5}]
             res2))

      (is (instance? ArrayList res1))
      (is (every? (fn [x]
                    (instance? HashMap x))
                  res2)))))


(deftest test-acc-as-index-by

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res1
          (pg/execute conn query {:as (fold/index-by :a)})

          res2
          (pg/execute conn query {:index-by :a})]

      (is (= {1 {:a 1 :b 2}
              3 {:a 3 :b 4}
              5 {:a 5 :b 6}}
             res1))

      (is (= {1 {:a 1 :b 2}
              3 {:a 3 :b 4}
              5 {:a 5 :b 6}}
             res2)))))


(deftest test-acc-as-group-by

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res1
          (pg/execute conn query {:as (fold/group-by :a)})

          res2
          (pg/execute conn query {:group-by :a})]

      (is (= {1 [{:a 1 :b 2}]
              3 [{:a 3 :b 4}]
              5 [{:a 5 :b 6}]}
             res1))

      (is (= {1 [{:a 1 :b 2}]
              3 [{:a 3 :b 4}]
              5 [{:a 5 :b 6}]}
             res2)))))


(deftest test-acc-as-transduce-into

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [tx
          (comp (map :a)
                (filter #{1 5})
                (map str))

          query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res1
          (pg/execute conn query {:as (fold/into tx)})

          res2
          (pg/execute conn query {:as (fold/into tx #{:a :b :c})})

          res3
          (pg/execute conn query {:into [tx []]})]

      (is (= ["1" "5"] res1))
      (is (= #{:a :b :c "1" "5"} res2))
      (is (= ["1" "5"] res3)))))


(deftest test-acc-as-edn

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          file1
          (File/createTempFile "test" ".edn")

          file2
          (File/createTempFile "test" ".edn")

          res1
          (with-open [out (io/writer file1)]
            (pg/execute conn query {:as (fold/to-edn out)}))

          res2
          (with-open [out (io/writer file2)]
            (pg/execute conn query {:to-edn out}))

          data1
          (-> file1 slurp read-string)

          data2
          (-> file2 slurp read-string)]

      (is (= 3 res1))
      (is (= 3 res2))

      (is (= [{:b 2, :a 1} {:b 4, :a 3} {:b 6, :a 5}]
             data1))

      (is (= [{:b 2, :a 1} {:b 4, :a 3} {:b 6, :a 5}]
             data2)))))


(deftest test-acc-as-json

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          file1
          (File/createTempFile "test" ".json")

          file2
          (File/createTempFile "test" ".json")

          res1
          (with-open [out (io/writer file1)]
            (pg/execute conn query {:as (fold/to-json out)}))

          res2
          (with-open [out (io/writer file2)]
            (pg/execute conn query {:to-json out}))

          data1
          (-> file1 io/reader json/read-reader)

          data2
          (-> file2 io/reader json/read-reader)]

      (is (= 3 res1))
      (is (= 3 res2))

      (is (= [{:b 2, :a 1} {:b 4, :a 3} {:b 6, :a 5}]
             data1))

      (is (= [{:b 2, :a 1} {:b 4, :a 3} {:b 6, :a 5}]
             data2)))))


(deftest test-acc-as-kv

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res1
          (pg/execute conn query {:as (fold/kv :b :a)})

          res2
          (pg/execute conn query {:kv [:b :a]})]

      (is (= {2 1
              4 3
              6 5}
             res1))

      (is (= {2 1
              4 3
              6 5}
             res2)))))


(deftest test-acc-as-run

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          capture!
          (atom [])

          func
          (fn [row]
            (swap! capture! conj row))

          res1
          (pg/execute conn query {:as (fold/run func)})

          res2
          (pg/execute conn query {:run func})]

      (is (= 3 res1))
      (is (= 3 res2))

      (is (= [{:b 2, :a 1}
              {:b 4, :a 3}
              {:b 6, :a 5}
              {:b 2, :a 1}
              {:b 4, :a 3}
              {:b 6, :a 5}]
             @capture!)))))


(deftest test-query-recover-from-exception

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [capture!
          (atom [])

          query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          func
          (fn [row]
            (/ 0 0)
            (if (= row {:a 5, :b 6})
              (/ 0 0)
              (swap! capture! conj row)))]

      (try
        (pg/execute conn query {:as (fold/run func)})
        (is false)
        (catch PGError e
          (is (= "Unhandled exception: Divide by zero" (-> e ex-message)))
          (is (= "Divide by zero" (-> e ex-cause ex-message)))))

      (testing "still can use the coll"
        (is (= :I (pg/status conn)))
        (is (= [{:one 1}] (pg/query conn "select 1 as one")))))))


(deftest test-acc-as-reduce

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          func
          (fn [acc {:keys [a b]}]
            (conj acc [a b]))

          res1
          (pg/execute conn query {:as (fold/reduce func #{})})

          res2
          (pg/execute conn query {:reduce [func #{}]})]

      (is (= #{[3 4] [5 6] [1 2]} res1))
      (is (= #{[3 4] [5 6] [1 2]} res2)))))


(deftest test-acc-as-table

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res1
          (pg/execute conn query {:as (fold/table)})

          res2
          (pg/execute conn query {:table true})]

      (is (= [[:a :b]
              [1 2]
              [3 4]
              [5 6]]
             res1))

      (is (= [[:a :b]
              [1 2]
              [3 4]
              [5 6]]
             res2)))))


(deftest test-acc-as-first

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res1
          (pg/execute conn query {:first true})

          res2
          (pg/execute conn query {:as fold/first})]

      (is (= {:a 1 :b 2} res1))
      (is (= {:a 1 :b 2} res2)))))


(deftest test-acc-as-map

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          func
          (fn [{:keys [a b]}]
            (+ a b))

          res1
          (pg/execute conn query {:map func})

          res2
          (pg/execute conn query {:as (fold/map func)})]

      (is (= [3 7 11] res1))
      (is (= [3 7 11] res2)))))


(deftest test-reducer-column

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res-keyword
          (pg/execute conn query {:as (fold/column :a)})

          res-miss
          (pg/execute conn query {:as (fold/column :lol)})

          res-string
          (pg/execute conn query {:fn-key identity
                                  :as (fold/column "a")})

          res-shortcut
          (pg/execute conn query {:fn-key identity
                                  :column "a"})]

      (is (= [1 3 5] res-keyword))
      (is (= [nil nil nil] res-miss))
      (is (= [1 3 5] res-string))
      (is (= [1 3 5] res-shortcut)))))


(deftest test-reducer-columns

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [query
          "with foo (a, b) as (values (1, 2), (3, 4), (5, 6)) select * from foo"

          res1
          (pg/execute conn query {:columns ["a" "b" "c"]
                                  :fn-key identity})

          res2
          (pg/execute conn query {:as (fold/columns ["a" "b" "c"])
                                  :fn-key identity})]

      (is (= [[1 2 nil] [3 4 nil] [5 6 nil]] res1))
      (is (= [[1 2 nil] [3 4 nil] [5 6 nil]] res2)))))


(deftest test-conn-params
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [params (pg/get-parameters conn)]
      (is (= {"IntervalStyle" "postgres"
              "client_encoding" "UTF8"
              "TimeZone" "Etc/UTC"
              "session_authorization" "test"
              "integer_datetimes" "on"
              "standard_conforming_strings" "on"
              "application_name" "pg2"
              "is_superuser" "on"
              "server_encoding" "UTF8"
              "DateStyle" "ISO, MDY"}
             (select-keys params
                          ["IntervalStyle"
                           "client_encoding"
                           "TimeZone"
                           "session_authorization"
                           "integer_datetimes"
                           "standard_conforming_strings"
                           "application_name"
                           "is_superuser"
                           "server_encoding"
                           "DateStyle"]))))))


(deftest test-two-various-params
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn
                      "select $1::int8 = $1::int4 as eq"
                      {:params [123]})]
      (is (= [{:eq true}] res)))))


(deftest test-pass-and-get-nil
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pg/execute conn
                      "select null as one, $1::int2 as two"
                      {:params [nil]})]
      (is (= [{:one nil, :two nil}] res))))

  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn
                      "select null as one, $1::int2 as two"
                      {:params [nil]})]
      (is (= [{:one nil, :two nil}] res)))))


(deftest test-execute-weird-param
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [stmt
          (pg/prepare conn "select $1::int8 = $1::int4 as eq")]

      (try
        (pg/execute-statement conn stmt {:params [(new Object)]})
        (is false)
        (catch PGError e
          (is (-> e ex-message (str/starts-with? "cannot text-encode a value")))))

      (let [res
            (pg/execute-statement conn stmt {:params [1]})]
        (is (= [{:eq true}] res))))))


(deftest test-statement-repr
  (let [repr
        "<Prepared statement, name: s1, param(s): 1, OIDs: [INT4], SQL: select $1::int4 as foo>"]
    (pg/with-connection [conn *CONFIG-TXT*]
      (pg/with-statement [stmt conn "select $1::int4 as foo"]
        (is (= repr (str stmt)))
        (is (= repr (with-out-str
                      (print stmt))))))))


(deftest test-empty-select
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select")]
      (is (= [{}] res)))))


(deftest test-encode-binary-simple
  (pg/with-connection [conn (assoc *CONFIG-TXT* :binary-encode? true)]
    (let [res (pg/execute conn "select $1::integer as num" {:params [42]})]
      (is (= [{:num 42}] res)))))


(deftest test-decode-binary-simple
  (pg/with-connection [conn (assoc *CONFIG-TXT* :binary-decode? true)]
    (let [res (pg/execute conn "select $1::integer as num" {:params [42]})]
      (is (= [{:num 42}] res)))))


(deftest test-decode-binary-unsupported
  (pg/with-connection [conn (assoc *CONFIG-TXT* :binary-decode? true)]
    (let [res (pg/execute conn "select '1 year 1 second'::interval as interval")]
      (is (= [{:interval [0 0 0 0 0 15 66 64 0 0 0 0 0 0 0 12]}]
             (update-in res [0 :interval] vec))))))


(deftest test-decode-text-unsupported
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select '1 year 1 second'::interval as interval")]
      (is (= [{:interval "1 year 00:00:01"}] res)))))


(deftest test-decode-binary-text
  (pg/with-connection [conn (assoc *CONFIG-TXT* :binary-decode? true)]
    (let [res (pg/execute conn "select 'hello'::text as text")]
      (is (= [{:text "hello"}] res)))))


(deftest test-decode-binary-varchar
  (pg/with-connection [conn (assoc *CONFIG-TXT* :binary-decode? true)]
    (let [res (pg/execute conn "select 'hello'::varchar as text")]
      (is (= [{:text "hello"}] res)))))


(deftest test-decode-text-and-binary-char
  (pg/with-connection [conn *CONFIG-BIN*]

    (let [res (pg/execute conn "select 'abc'::\"char\" as char")]
      (is (= [{:char \a}] res)))

    (let [res (pg/execute conn "select ''::\"char\" as char")]
      (is (= [{:char (char 0)}] res)))

    (let [res (pg/execute conn "select $1 as char" {:params [\a]
                                                    :oids [oid/char]})]
      (is (= [{:char \a}] res))))

  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? false
                                   :binary-decode? false)]

    (let [res (pg/execute conn "select 'abc'::\"char\" as char")]
      (is (= [{:char \a}] res)))

    (let [res (pg/execute conn "select ''::\"char\" as char")]
      (is (= [{:char Character/MIN_VALUE}] res)))

    (let [res (pg/execute conn "select $1 as char" {:params [\a]
                                                    :oids [oid/char]})]
      (is (= [{:char \a}] res)))))


(deftest test-decode-oid
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res (pg/execute conn "select $1::oid as oid" {:params [42]})]
      (is (= [{:oid 42}] res)))))


(deftest test-decode-oid-binary
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res (pg/execute conn "select $1::oid as oid" {:params [42]})]
      (is (= [{:oid 42}] res)))))


(deftest test-uuid-text
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [uuid
          (random-uuid)
          res
          (pg/execute conn "select $1::uuid as uuid" {:params [uuid]})]
      (is (= [{:uuid uuid}] res)))))


(deftest test-uuid-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [uuid
          (random-uuid)
          res
          (pg/execute conn "select $1::uuid as uuid" {:params [uuid]})]
      (is (= [{:uuid uuid}] res)))))


(deftest test-time-bin-read
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn "select '12:01:59.123456789+03'::time as time")
          time
          (-> res first :time)]
      (is (instance? LocalTime time))
      (is (= "12:01:59.123457" (str time))))))


(deftest test-timetz-bin-read
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn "select '12:01:59.123456789+03'::timetz as timetz")
          timetz
          (-> res first :timetz)]
      (is (instance? OffsetTime timetz))
      (is (= "12:01:59.123457+03:00" (str timetz))))))


(deftest test-timestamp-bin-read
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn "select '2022-01-01 12:01:59.123456789+03'::timestamp as ts")
          ts
          (-> res first :ts)]
      (is (instance? LocalDateTime ts))
      (is (= "2022-01-01T12:01:59.123457" (str ts))))))


(deftest test-timestamptz-bin-read
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn "select '2022-01-01 12:01:59.123456789+03'::timestamptz as tstz")
          tstz
          (-> res first :tstz)]
      (is (instance? OffsetDateTime tstz))
      (is (= "2022-01-01T09:01:59.123457Z" (str tstz))))))


(deftest test-date-bin-read
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res
          (pg/execute conn "select '2022-01-01 12:01:59.123456789+03'::date as date")
          date
          (-> res first :date)]
      (is (instance? LocalDate date))
      (is (= "2022-01-01" (str date))))))


(deftest test-pass-zoned-time-timetz-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [x1
          (OffsetTime/now)

          res
          (pg/execute conn "select $1 as x" {:params [x1]
                                             :oids [oid/timetz]})

          x2
          (-> res first :x)]

      (is (instance? OffsetTime x2))
      (is (= x1 x2)))))


(deftest test-pass-local-time-time-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [x1
          (LocalTime/now)

          res
          (pg/execute conn "select $1::time as x;" {:params [x1]})

          x2
          (-> res first :x)]

      (is (instance? LocalTime x2))
      (is (= x1 x2)))))


(deftest test-pass-instant-timestamptz-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [x1
          (Instant/now)

          res
          (pg/execute conn "select $1::timestamptz as x;" {:params [x1]})

          ^OffsetDateTime x2
          (-> res first :x)]

      (is (= x1 (.toInstant x2))))))


(deftest test-pass-instant-timestamp-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [x1
          (Instant/parse "2023-07-25T12:36:15.981981Z")

          res
          (pg/execute conn "select $1::timestamp as x" {:params [x1]})

          x2
          (-> res first :x)]

      (is (= (LocalDateTime/parse "2023-07-25T12:36:15.981981")
             x2)))))


(deftest test-pass-date-timestamp-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [x1
          (new Date 123123123123123)
          ;; 5871-08-14T03:32:03.123-00:00

          res
          (pg/execute conn "select $1::timestamp as x" {:params [x1]})

          x2
          (-> res first :x)]

      (is (= "5871-08-14T03:32:03.123" (str x2))))))


(deftest test-pass-date-timestamptz-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [x1
          (new Date 123123123123123)
          ;; 5871-08-14T03:32:03.123-00:00

          res
          (pg/execute conn "select $1::timestamptz as x" {:params [x1]})

          x2
          (-> res first :x)]

      (is (= "5871-08-14T03:32:03.123Z" (str x2))))))


(deftest test-read-write-numeric-txt
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [x1
          (bigdec "-123.456")

          res
          (pg/execute conn "select $1::numeric as x" {:params [x1]})

          x2
          (-> res first :x)]

      (is (= (str x1) (str x2))))))


(deftest test-read-write-numeric-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [x1
          (bigdec "-123.456")

          res
          (pg/execute conn "select $1::numeric as x" {:params [x1]})

          x2
          (-> res first :x)]

      (is (= (str x1) (str x2))))))


(deftest test-with-timeout-no-cancell
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-timeout [conn 2000]
      (let [res
            (pg/query conn "select pg_sleep(1) as sleep")]
        (is (= [{:sleep ""}] res))))
    (testing "ensure it has been cancelled"
      (let [res
            (pg/query conn "select pg_sleep(2) as sleep")]
        (is (= [{:sleep ""}] res))))))


(deftest test-with-timeout-do-cancell
  (pg/with-connection [conn *CONFIG-TXT*]
    (pg/with-timeout [conn 100]
      (try
        (pg/query conn "select pg_sleep(999) as sleep")
        (is false)
        (catch PGErrorResponse e
          (is true)
          (is (-> e ex-message (str/includes? "canceling statement due to user request"))))))
    (testing "ensure it has been cancelled"
      (let [res
            (pg/query conn "select pg_sleep(1) as sleep")]
        (is (= [{:sleep ""}] res))))))


(deftest test-cancel-query

  (let [conn1
        (pg/connect *CONFIG-TXT*)

        fut
        (future
          (pg/query conn1 "select pg_sleep(60) as sleep"))

        _
        ;; let it start
        (Thread/sleep 100)

        res
        (pg/cancel-request conn1)

        res2
        (pg/query conn1 "select 42 as res")]

    (is (nil? res))

    (try
      @fut
      (is false)
      (catch ExecutionException e-future
        (let [e (ex-cause e-future)]
          (is (str/includes? (ex-message e)
                             "message=canceling statement due to user request")))))

    (is (= [{:res 42}] res2))))


(deftest test-copy-out-broken-stream

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [sql
          "copy (select s.x as x, s.x * s.x as square from generate_series(1, 9) as s(x)) TO STDOUT WITH (FORMAT CSV)"

          out
          (proxy [OutputStream] []
            (write [b]
              (throw (new Exception "BOOM"))))]

      (try
        (pg/copy-out conn sql out)
        (is false)
        (catch PGError e
          (is (= "Unhandled exception: BOOM"
                 (-> e ex-message)))
          (is (= "BOOM" (-> e ex-cause ex-message)))))

      (testing "the connection still can be used"
        (is (= :I (pg/status conn)))
        (is (= [{:one 1}]
               (pg/query conn "select 1 as one")))))))


(deftest test-copy-out-api-txt

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [sql
          "copy (select s.x as x, s.x * s.x as square from generate_series(1, 9) as s(x)) TO STDOUT WITH (FORMAT CSV)"

          out
          (new ByteArrayOutputStream)

          res
          (pg/copy-out conn sql out)

          rows
          (with-open [reader (-> out
                                 (.toByteArray)
                                 (io/input-stream)
                                 (io/reader))]
            (vec (csv/read-csv reader)))]

      (is (= {:copied 9} res))

      (is (= [["1" "1"]
              ["2" "4"]
              ["3" "9"]
              ["4" "16"]
              ["5" "25"]
              ["6" "36"]
              ["7" "49"]
              ["8" "64"]
              ["9" "81"]] rows)))))


(deftest test-copy-out-api-bin

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [sql
          "copy (select s.x as x, s.x * s.x as square from generate_series(1, 3) as s(x)) TO STDOUT WITH (FORMAT BINARY)"

          out
          (new ByteArrayOutputStream)

          res
          (pg/copy-out conn sql out)]

      (is (= {:copied 3} res))

      (is (= [80 71 67 79 80 89 10 -1 13 10 0 0 0 0 0 0 0 0 0 0 2 0 0 0 4 0 0 0 1 0 0 0 4 0 0 0 1 0 2 0 0 0 4 0 0 0 2 0 0 0 4 0 0 0 4 0 2 0 0 0 4 0 0 0 3 0 0 0 4 0 0 0 9 -1 -1]
             (-> out (.toByteArray) (vec)))))))


(deftest test-copy-out-api-multiple-expressions

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [sql
          "select 42; copy (select s.x as x, s.x * s.x as square from generate_series(1, 9) as s(x)) TO STDOUT WITH (FORMAT CSV)"

          out
          (new ByteArrayOutputStream)

          _
          (pg/copy-out conn sql out)]

      (is (= "1,1\n2,4\n3,9\n4,16\n5,25\n6,36\n7,49\n8,64\n9,81\n"
             (-> out .toByteArray String.))))

    (let [res (pg/query conn "select 1 as one")]
      (is (= [{:one 1}] res)))))


(deftest test-copy-out-query

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [sql
          "
copy (select s.x as x, s.x * s.x as square from generate_series(1, 4) as s(x)) TO STDOUT WITH (FORMAT BINARY);
select 1 as one;
copy (select s.x as X from generate_series(1, 3) as s(x)) TO STDOUT WITH (FORMAT CSV);
          "

          out1
          (new ByteArrayOutputStream)

          out2
          (new ByteArrayOutputStream)

          output-streams
          [out1 out2]

          res
          (pg/query conn sql)

          dump1
          (.toByteArray out1)

          dump2
          (.toByteArray out2)]

      (is (= 0 (count dump1)))
      (is (= 0 (count dump2)))

      (is (= [{:copied 4}
              [{:one 1}]
              {:copied 3}]
             res)))))


(deftest test-copy-in-stream-csv

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id bigint, name text, active boolean)")

    (let [rows
          [[1 "Ivan" true]
           [2 "Juan" false]]

          out
          (new ByteArrayOutputStream)

          _
          (with-open [writer (io/writer out)]
            (csv/write-csv writer rows))

          in-stream
          (-> out .toByteArray io/input-stream)

          res-copy
          (pg/copy-in conn
                      "copy foo (id, name, active) from STDIN WITH (FORMAT CSV)"
                      in-stream)

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 2} res-copy))

      (is (= [{:id 1 :name "Ivan" :active true}
              {:id 2 :name "Juan" :active false}]
             res-query)))))


(deftest test-copy-in-stream-csv-broken-stream

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id bigint, name text, active boolean)")

    (let [in-stream
          (proxy [InputStream] []
            (read [buf]
              (throw (new Exception "BOOM"))))]

      (try
        (pg/copy-in conn
                    "copy foo (id, name, active) from STDIN WITH (FORMAT CSV)"
                    in-stream)
        (is false)
        (catch PGError e
          (is (= "Unhandled exception: BOOM" (-> e ex-message)))
          (is (= "BOOM" (-> e ex-cause ex-message)))))

      (testing "the connection is still usable"
        (is (= :I (pg/status conn)))
        (is (= [{:one 1}] (pg/query conn "select 1 as one")))))))


(deftest test-copy-in-rows-exception-in-the-middle

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id bigint, name text, active boolean, note text)")

    (let [rows
          [[1 "Ivan" true (new Object)]]]

      (try
        (pg/copy-in-rows conn
                         "copy foo (id, name, active, note) from STDIN WITH (FORMAT CSV, NULL 'dummy', DELIMITER '|')"
                         rows)
        (is false)
        (catch PGError e
          (is (-> e
                  ex-message
                  (str/starts-with? "Unhandled exception: cannot text-encode a value")))
          (is (-> e
                  ex-cause
                  ex-message
                  (str/starts-with? "cannot text-encode a value"))))))

    (testing "conn is ok"
      (is (= :I (pg/status conn)))
      (is (= [{:one 1}] (pg/query conn "select 1 as one"))))))


(deftest test-copy-in-rows-ok-csv

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id bigint, name text, active boolean, note text)")

    (let [weird
          "foo'''b'ar\r\n\f\t\bsdf--NULL~!@#$%^&*()\"sdf\"\""

          rows
          [[1 "Ivan" true weird]
           [2 "Juan" false nil]]

          res-copy
          (pg/copy-in-rows conn
                           "copy foo (id, name, active, note) from STDIN WITH (FORMAT CSV, NULL 'dummy', DELIMITER '|')"
                           rows
                           {:csv-null "dummy"
                            :csv-sep "|"})

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 2} res-copy))

      (is (= [{:id 1 :name "Ivan" :active true :note weird}
              {:id 2 :name "Juan" :active false :note nil}]
             res-query)))))


(deftest test-copy-in-rows-null-values

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id bigint, name text, active boolean, note text)")

    (let [rows
          [[1 "Ivan" true nil]
           [2 "Juan" false nil]]

          res-copy
          (pg/copy-in-rows conn
                           "copy foo (id, name, active, note) from STDIN WITH (FORMAT BINARY)"
                           rows
                           {:copy-bin? true})

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 2} res-copy))

      (is (= [{:id 1 :name "Ivan" :active true :note nil}
              {:id 2 :name "Juan" :active false :note nil}]
             res-query)))))


(deftest test-copy-in-rows-ok-csv-wrong-oids

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2)")

    (try
      (pg/copy-in-rows conn
                       "copy foo (id) from STDIN WITH (FORMAT CSV)"
                       [[1] [2] [3]]
                       {:oids [oid/uuid]})
      (is false)
      (catch PGError e
        (is (= "Unhandled exception: cannot text-encode a value: 1, OID: UUID, type: java.lang.Long"
               (ex-message e)))))))


(deftest test-copy-in-rows-ok-bin

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2, name text, active boolean, note text)")

    (let [rows
          [[1 "Ivan" true nil]
           [2 "Juan" false "kek"]]

          res-copy
          (pg/copy-in-rows conn
                           "copy foo (id, name, active, note) from STDIN WITH (FORMAT BINARY)"
                           rows
                           {:copy-bin? true
                            :oids [oid/int2 oid/default oid/bool]})

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 2} res-copy))

      (is (= [{:id 1 :name "Ivan" :active true :note nil}
              {:id 2 :name "Juan" :active false :note "kek"}]
             res-query)))))


(deftest test-copy-in-broken-csv

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id bigint, name text, active boolean)")

    (let [in-stream
          (-> "\n\b232\t\n\n@#^@#$\r\b"
              (.getBytes)
              io/input-stream)]

      (try
        (pg/copy-in conn
                    "copy foo (id, name, active) from STDIN WITH (FORMAT CSV)"
                    in-stream
                    {:copy-buf-size 1})
        (is false)
        (catch PGErrorResponse e
          (is true)
          (is (-> e ex-message (str/includes? "missing data for column")))))

      (let [res-query
            (pg/query conn "select 1 as one")]
        (is (= [{:one 1}] res-query))))))


(deftest test-copy-in-maps-ok-csv

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2, name text, active boolean, note text)")

    (let [weird
          "foo'''b'ar\r\n\f\t\bsdf--NULL~!@#$%^&*()\"sdf\"\""

          maps
          [{:id 1 :name "Ivan" :active true :note "aaa"}
           {:aaa false :id 2 :active nil :note nil :name "Juan" :extra "Kek" :lol 123}]

          res-copy
          (pg/copy-in-maps conn
                           "copy foo (id, name, active, note) from STDIN WITH (FORMAT CSV)"
                           maps
                           [:id :name :active :note]
                           {:oids [oid/int2 nil oid/bool nil nil nil nil]
                            :copy-csv? true})

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 2} res-copy))

      (is (= [{:id 1, :name "Ivan", :active true, :note "aaa"}
              {:id 2, :name "Juan", :active nil, :note nil}]
             res-query)))))


(deftest test-copy-in-maps-wrong-oid

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2, name text, active boolean, note text)")

    (let [weird
          "foo'''b'ar\r\n\f\t\bsdf--NULL~!@#$%^&*()\"sdf\"\""

          maps
          [{:id 1 :name "Ivan" :active true :note "aaa"}
           {:aaa false :id 2 :active nil :note nil :name "Juan" :extra "Kek" :lol 123}]

          _
          (try
            (pg/copy-in-maps conn
                             "copy foo (id, name, active, note) from STDIN WITH (FORMAT CSV)"
                             maps
                             [:id :name :active :note]
                             {:oids [oid/int2 nil oid/float4 nil nil nil nil]
                              :copy-csv? true})
            (is false)
            (catch PGError e
              (is true)))

          res-query
          (pg/query conn "select * from foo")]

      (is (= [{:foo 42}] (pg/query conn "select 42 as foo"))))))


(deftest test-copy-in-maps-ok-bin

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2, name text, active boolean, note text)")

    (let [weird
          "foo'''b'ar\r\n\f\t\bsdf--NULL~!@#$%^&*()\"sdf\"\""

          maps
          [{:lala 123 :name "Ivan" :id 1 :active true :note "aaa"}
           {:id 2 :active nil :note nil :name "Juan" :extra "Kek"}]

          res-copy
          (pg/copy-in-maps conn
                           "copy foo (id, name, active, note) from STDIN WITH (FORMAT BINARY)"
                           maps
                           [:id :name :active :note]
                           {:oids [oid/int2]
                            :copy-bin? true})

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 2} res-copy))

      (is (= [{:id 1, :name "Ivan", :active true, :note "aaa"}
              {:id 2, :name "Juan", :active nil, :note nil}]
             res-query)))))


(deftest test-copy-in-maps-error-in-the-middle

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2, name text, active boolean, note text)")

    (let [maps
          [{:lala 123 :name "Ivan" :id 1 :active true :note "aaa"}
           {:id 2 :active nil :note nil :name (new Object) :extra "lol"}]]

      (try
        (pg/copy-in-maps conn
                         "copy foo (id, name, active, note) from STDIN WITH (FORMAT BINARY)"
                         maps
                         [:id :name :active :note]
                         {:oids [oid/int2]
                          :copy-bin? true})
        (is false)
        (catch PGError e
          (is (-> e
                  ex-message
                  (str/starts-with? "Unhandled exception: cannot binary-encode a value")))
          (is (-> e
                  ex-cause
                  ex-message
                  (str/starts-with? "cannot binary-encode a value"))))))

    (testing "the conn still works"
      (is (= :I (pg/status conn)))
      (is (= [{:one 1}] (pg/query conn "select 1 as one"))))))


(deftest test-copy-in-rows-empty-csv

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2, name text, active boolean, note text)")

    (let [res-copy
          (pg/copy-in-rows conn
                           "copy foo (id, name, active, note) from STDIN WITH (FORMAT CSV)"
                           nil
                           {:oids [oid/int2]
                            :copy-csv? true})

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 0} res-copy))
      (is (= [] res-query)))))


(deftest test-copy-in-maps-some-rows-null

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2, name text, active boolean, note text)")

    (let [weird
          "foo'''b'ar\r\n\f\t\bsdf--NULL~!@#$%^&*()\"sdf\"\""

          maps
          [{:lala 123 :name "Ivan" :id 1 :active true :note "aaa"}
           nil
           {:id 2 :active nil :note nil :name "Juan" :extra "Kek"}]

          res-copy
          (pg/copy-in-maps conn
                           "copy foo (id, name, active, note) from STDIN WITH (FORMAT BINARY)"
                           maps
                           [:id :name :active :note]
                           {:oids [oid/int2]
                            :copy-format pg/COPY_FORMAT_BIN})

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 2} res-copy))

      (is (= [{:id 1, :name "Ivan", :active true, :note "aaa"}
              {:id 2, :name "Juan", :active nil, :note nil}]
             res-query)))))


(deftest test-copy-in-maps-empty-bin

  (pg/with-connection [conn *CONFIG-TXT*]

    (pg/query conn "create temp table foo (id int2, name text, active boolean, note text)")

    (let [res-copy
          (pg/copy-in-maps conn
                           "copy foo (id, name, active, note) from STDIN WITH (FORMAT BINARY)"
                           []
                           [:id :name :active :note]
                           {:oids [oid/int2]
                            :copy-bin? true})

          res-query
          (pg/query conn "select * from foo")]

      (is (= {:copied 0} res-copy))
      (is (= [] res-query)))))


(deftest test-client-array-read-bin-simple
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res (pg/execute conn
                          "select '{1,2,3}'::int[] as arr"
                          {:first true})]
      (is (= {:arr [1, 2, 3]}
             (update res :arr vec))))))


(deftest test-client-array-read-bin-multi
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [res (pg/execute conn
                          "select '{{1,2,3},{4,5,6}}'::int[][] as arr"
                          {:first true})]
      (is (= {:arr [[1, 2, 3], [4, 5, 6]]}
             (-> res
                 (update :arr vec)
                 (update-in [:arr 0] vec)
                 (update-in [:arr 1] vec)))))))


(deftest test-array-read-bin
  (pg/with-connection [conn (assoc *CONFIG-TXT* :binary-decode? true)]

    (let [res (pg/execute conn "select '{1,2,3}'::int[] as array")]
      (is (= [{:array [1 2 3]}] res)))

    (let [res (pg/execute conn "select '{foo,null,baz}'::text[] as array")]
      (is (= [{:array ["foo" nil "baz"]}] res)))

    (let [res (pg/execute conn "select '{{{1,2,3},{4,5,6}},{{7,8,9},{10,11,12}}}'::text[] as array")]
      (is (= [{:array
               [[["1" "2" "3"] ["4" "5" "6"]]
                [["7" "8" "9"] ["10" "11" "12"]]]}]
             res)))

    (let [res (pg/execute conn "select '{true,false,null,false,true}'::bool[] as array")]
      (is (= [{:array [true false nil false true]}]
             res)))

    (let [res (pg/execute conn "select '{10:00,12:00,23:59}'::time[] as array")]
      (is (= [{:array
               [(LocalTime/parse "10:00")
                (LocalTime/parse "12:00")
                (LocalTime/parse "23:59")]}]
             res)))

    (let [res (pg/execute conn "select '{{2020-01-01,2021-12-31},{2099-11-03,1301-01-23}}'::date[][] as array")]
      (is (= [{:array
               [[(LocalDate/parse "2020-01-01")
                 (LocalDate/parse "2021-12-31")]
                [(LocalDate/parse "2099-11-03")
                 (LocalDate/parse "1301-01-23")]]}]
             res)))

    (let [res (pg/execute conn "select '{{887dfa2b-ab88-47d6-ab2f-83b66685063e,9ae401db-95ee-4612-880c-011ad15cdacf},{2f15d54b-836d-426a-9389-b878f6b0aa18,88991362-20ff-4217-96d5-20bd70166916}}'::uuid[][] as array")]
      (is (= [{:array
               [[#uuid "887dfa2b-ab88-47d6-ab2f-83b66685063e"
                 #uuid "9ae401db-95ee-4612-880c-011ad15cdacf"]
                [#uuid "2f15d54b-836d-426a-9389-b878f6b0aa18"
                 #uuid "88991362-20ff-4217-96d5-20bd70166916"]]}]
             res)))))


(deftest test-array-weird-text-bin
  (pg/with-connection [conn *CONFIG-BIN*]
    (let [weird-word
          "\"{}(),,'''\" !@#$%^&*()_\\+AS<>??~\\\\sfd \\\r\n\t\bsdf"

          arr
          [["aaa" weird-word] [nil nil]]

          res
          (pg/execute conn
                      "select $1::text[][] as arr"
                      {:params [arr]})]

      (is (= [{:arr arr}] res)))))


(deftest test-array-weird-text-txt
  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? false
                                   :binary-decode? false)]
    (let [weird-word
          "\"{}(),,'''\" !@#$%^&*()_\\+AS<>??~\\\\sfd \\\r\n\t\bsdf"

          arr
          [["aaa" weird-word] [nil nil]]

          res
          (pg/execute conn
                      "select $1::text[][] as arr"
                      {:params [arr]})]

      (is (= [{:arr arr}] res)))))


(deftest test-array-null-string-bin-txt
  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? true
                                   :binary-decode? false)]
    (let [arr [nil "null" "NULL" nil "!!@#$%^&*()\"\\{}[]--`\r\b\n\f\tkek"]
          res (pg/execute conn
                          "select $1::text[] as arr"
                          {:params [arr]})]
      (is (= [{:arr arr}] res)))))


(deftest test-array-null-string-txt-bin
  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? false
                                   :binary-decode? true)]
    (let [arr [nil "null" "NULL" nil "!!@#$%^&*()\"\\{}[]--`\r\b\n\f\tkek"]
          res (pg/execute conn
                          "select $1::text[] as arr"
                          {:params [arr]})]
      (is (= [{:arr arr}] res)))))


(deftest test-array-multi-dim-bin-txt
  (pg/with-connection [conn (assoc *CONFIG-TXT*
                                   :binary-encode? true
                                   :binary-decode? false)]
    (let [arr
          [[(LocalTime/parse "10:00")
            (LocalTime/parse "11:00")
            (LocalTime/parse "12:00")]
           [(LocalTime/parse "10:01")
            (LocalTime/parse "11:01")
            (LocalTime/parse "12:01")]
           [(LocalTime/parse "10:02")
            (LocalTime/parse "11:02")
            (LocalTime/parse "12:02")]]
          res (pg/execute conn "select $1::time[] as arr" {:params [arr]})]
      (is (= [{:arr arr}] res)))))


(deftest test-array-in-array
  (pg/with-connection [conn *CONFIG-TXT*]
    (let [arr [1 2 3]
          res (pg/execute conn
                          "select 2 = ANY ($1) as in_array"
                          {:params [arr]})]
      (is (= [{:in_array true}] res)))))


(deftest test-honey-query

  (pg/with-connection [conn *CONFIG-TXT*]
    (let [res
          (pgh/query conn
                     {:select [[[:inline "string"] :foo]]}
                     {:first true
                      :honey {:pretty true}})]
      (is (= {:foo "string"} res)))))


(deftest test-honey-execute

  (pg/with-connection [conn *CONFIG-TXT*]

    (let [table
          (gen-table)

          query
          (format "create temp table %s (id serial, title text)" table)

          _
          (pg/execute conn query)

          res1
          (pgh/execute conn
                       {:insert-into (keyword table)
                        :values [{:id 1 :title "test1"}
                                 {:id 2 :title "test2"}
                                 {:id 3 :title "test3"}]}
                       {:honey
                        {:pretty true}})

          res2
          (pgh/execute conn
                       {:select [:id :title]
                        :from [(keyword table)]
                        :where [:and
                                [:= :id 2]
                                [:= :title [:param :title]]]}
                       {:honey
                        {:pretty true
                         :params {:title "test2"}}})]

      (is (= {:inserted 3} res1))
      (is (= [{:id 2 :title "test2"}] res2)))))


(deftest test-component-connection

  (let [c-init
        (pgc/connection *CONFIG-TXT*)

        c-started
        (component/start c-init)

        res
        (pg/query c-started "select 1 as one" {:first true})

        c-stopped
        (component/stop c-started)]

    (is (map? c-init))
    (is (pg/connection? c-started))
    (is (= {:one 1} res))
    (is (nil? c-stopped))))


(deftest test-component-pool

  (let [c-init
        (pgc/pool *CONFIG-TXT*)

        c-started
        (component/start c-init)

        res
        (pool/with-connection [conn c-started]
          (pg/query conn "select 1 as one" {:first true}))

        c-stopped
        (component/stop c-started)]

    (is (map? c-init))
    (is (pool/pool? c-started))
    (is (= {:one 1} res))
    (is (nil? c-stopped))))
