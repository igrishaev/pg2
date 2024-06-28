(ns pg.jdbc-test
  (:import
   org.pg.error.PGError
   java.time.LocalDate)
  (:require
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.integration :refer [*DB-SPEC* P15]]
   [pg.oid :as oid]
   [pg.jdbc :as jdbc]
   [pg.pool :as pool]))


(def CONFIG
  (assoc *DB-SPEC* :port P15))


(deftest test-get-connection-map
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute! conn ["select 1 as one"])]

      (is (pg/connection? conn))
      (is (= [{:one 1}] res)))))


(deftest test-get-connection-pool
  (with-open [pool (pool/pool CONFIG)
              conn (jdbc/get-connection pool)]
    (let [res
          (jdbc/execute! conn ["select 1 as one"])]
    (is (pg/connection? conn))
    (is (= [{:one 1}] res)))))


(deftest test-get-connection-error
  (try
    (jdbc/get-connection nil)
    (is false)
    (catch Throwable e
      (is true)
      (is (= "Connection source cannot be null"
             (ex-message e))))))


(deftest test-execute!-conn
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute! conn ["select $1::int4 as num" 42])]
      (is (= [{:num 42}] res)))))


(deftest test-execute!-kebab-off
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute! conn
                         ["select $1::int as foo_bar" 42]
                         {:kebab-keys? false})]
      (is (= [{:foo_bar 42}] res)))))


(deftest test-execute!-conn-opt
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute! conn
                         ["select $1::int4 as num, $2::bool" 42 true]
                         {:table true})]
      (is (= [[:num :bool] [42 true]] res))
      (pg/close conn))))


(deftest test-execute!-pool-opt
  (pool/with-pool [pool CONFIG]
    (let [res
          (jdbc/execute! pool
                         ["select $1::int4 as num, $2::bool" 42 true]
                         {:table true})]
      (is (= [[:num :bool] [42 true]] res)))))


(deftest test-prepare-conn
  (with-open [conn (jdbc/get-connection CONFIG)]

    (let [stmt
          (jdbc/prepare conn
                        ["select $1 as num, $2 as bool" 42 true]
                        {:oids [oid/int4 oid/bool]})

          res
          (jdbc/execute! conn
                         [stmt 123 false]
                         {:table true})]

      (is (pg/prepared-statement? stmt))
      (is (= [[:num :bool] [123 false]] res)))))


(deftest test-prepare-pool
  (with-open [pool (pool/pool CONFIG)
              conn (jdbc/get-connection pool)]

    (let [stmt
          (jdbc/prepare conn
                        ["select $1 as num, $2 as bool" 42 true]
                        {:oids [oid/int4 oid/bool]})

          res
          (jdbc/execute! conn
                         [stmt 123 false]
                         {:table true})]

      (is (pg/prepared-statement? stmt))
      (is (= [[:num :bool] [123 false]] res)))))


(deftest test-execute-one!-conn
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute-one! conn
                             ["select $1::int as foo_bar" 42])]
      (is (= {:foo-bar 42} res)))))


(deftest test-execute-one!-pool
  (with-open [pool (pool/pool CONFIG)
              conn (jdbc/get-connection pool)]
    (let [res
          (jdbc/execute-one! conn
                             ["select $1::bool as foo_bar" true])]
      (is (= {:foo-bar true} res)))))


(deftest test-execute-one!-conn-stmt
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [date
          (LocalDate/parse "2024-01-30")

          stmt
          (jdbc/prepare conn
                        ["select $1 as is_date, $2 as is_num"]
                        {:oids [oid/date oid/int4]})

          res
          (jdbc/execute-one! conn
                             [stmt date 123])]

      (is (= {:is-num 123
              :is-date date} res)))))


(deftest test-execute-one!-kebab-off
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute-one! conn
                             ["select $1::int as foo_bar" 42]
                             {:kebab-keys? false})]
      (is (= {:foo_bar 42} res)))))


(deftest test-execute-batch!
  (with-open [conn (jdbc/get-connection CONFIG)]
    (try
      (jdbc/execute-batch! conn "select 1" [])
      (is false)
      (catch PGError e
        (is true)
        (is (= "execute-batch! is not imiplemented"
               (ex-message e)))))))


(deftest test-on-connection-conn
  (jdbc/on-connection [conn CONFIG]
    (is (pg/connection? conn))
    (is (= [{:num 1}]
           (pg/query conn "select 1 as num")))))


(deftest test-on-connection-pool
  (pool/with-pool [pool CONFIG]
    (jdbc/on-connection [conn pool]
      (is (pg/connection? conn))
      (is (= [{:num 1}]
             (pg/query conn "select 1 as num"))))))


(deftest test-transact-config
  (let [func
        (fn [conn]
          (jdbc/execute! conn
                         ["select $1::int as one" 42]))

        res
        (jdbc/transact CONFIG
                       func
                       {:isolation :serializable
                        :read-only true
                        :rollback-only true})]

    (is (= [{:one 42}] res))))


(deftest test-transact-pool
  (with-open [pool (pool/pool CONFIG)]

    (let [func
          (fn [conn]
            (jdbc/execute! conn
                           ["select $1::int as one" 42]))

          res
          (jdbc/transact pool
                         func
                         {:isolation :serializable
                          :read-only true
                          :rollback-only true})]

      (is (= [{:one 42}] res)))))


(deftest test-with-transaction-config
  (let [opts
        {:isolation :serializable
         :read-only true
         :rollback-only true}

        conn!
        (atom nil)]

    (jdbc/with-transaction [conn CONFIG opts]
      (reset! conn! conn)
      (let [res
            (jdbc/execute! conn
                           ["select $1::int as one" 42])]
        (is (= [{:one 42}] res))))

    (is (pg/connection? @conn!))
    (is (pg/closed? @conn!))))


(deftest test-with-transaction-conn
  (let [opts
        {:isolation :serializable
         :read-only true
         :rollback-only true}
        conn!
        (atom nil)]
    (pg/with-connection [foo CONFIG]
      (reset! conn! foo)
      (jdbc/with-transaction [TX foo opts]
        (let [res
              (jdbc/execute! TX
                             ["select $1::int as one" 42])]
          (is (= [{:one 42}] res)))))
    (is (pg/connection? @conn!))
    (is (pg/closed? @conn!))))


(deftest test-with-transaction-pool
  (let [opts
        {:isolation :serializable
         :read-only true
         :rollback-only true}]
    (pool/with-pool [foo CONFIG]
      (let [stats1
            (pool/stats foo)]

        (jdbc/with-transaction [TX foo opts]

          (let [stats2
                (pool/stats foo)

                tx?
                (jdbc/active-tx? TX)

                res
                (jdbc/execute! TX
                               ["select $1::int as one" 42])]

            (is (= {:free 1 :used 1} stats2))

            (is tx?)
            (is (= [{:one 42}] res))))

        (let [stats3
              (pool/stats foo)]

          (is (= {:free 2 :used 0} stats1))
          (is (= {:free 2 :used 0} stats3)))))))
