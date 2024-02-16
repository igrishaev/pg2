(ns pg.jdbc-test
  (:require
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.integration :refer [*CONFIG* P15]]
   [pg.oid :as oid]
   [pg.jdbc :as jdbc]
   [pg.pool :as pool]))


(def CONFIG
  (assoc *CONFIG* :port P15))


(deftest test-get-connection-map
  (let [conn
        (jdbc/get-connection CONFIG)

        res
        (jdbc/execute! conn ["select 1 as one"])]

    (is (pg/connection? conn))
    (is (= [{:one 1}] res))

    (pg/close conn)))


(deftest test-get-connection-pool
  (let [pool
        (pool/pool CONFIG)

        conn
        (jdbc/get-connection pool)

        res
        (jdbc/execute! conn ["select 1 as one"])]

    (is (pg/connection? conn))
    (is (= [{:one 1}] res))

    (pool/close pool)))


(deftest test-get-connection-error
  (try
    (jdbc/get-connection nil)
    (is false)
    (catch Throwable e
      (is true)
      (is (= "Unsupported connection source: null" (ex-message e))))))


(deftest test-execute!-conn
  (let [conn
        (jdbc/get-connection CONFIG)
        res
        (jdbc/execute! conn ["select $1::int4 as num" 42])]
    (is (= [{:num 42}] res))))


(deftest test-execute!-conn-opt
  (let [conn
        (jdbc/get-connection CONFIG)
        res
        (jdbc/execute! conn
                       ["select $1::int4 as num, $2::bool" 42 true]
                       {:matrix? true})]
    (is (= [[:num :bool] [42 true]] res))
    (pg/close conn)))


(deftest test-execute!-pool-opt
  (pool/with-pool [pool CONFIG]
    (let [res
          (jdbc/execute! pool
                         ["select $1::int4 as num, $2::bool" 42 true]
                         {:matrix? true})]
      (is (= [[:num :bool] [42 true]] res)))))


(deftest test-prepare-conn
  (let [conn
        (jdbc/get-connection CONFIG)

        stmt
        (jdbc/prepare conn
                      ["select $1 as num, $2 as bool" 42 true]
                      {:oids [oid/int4]})

        res
        (jdbc/execute! conn
                       [stmt 123 false]
                       {:matrix? true})]

    (is (pg/prepared-statement? stmt))
    (is (= [[:num :bool] [123 false]] res))

    (pg/close conn)))


(deftest test-prepare-pool
  (pool/with-pool [pool CONFIG]
    (let [conn
          (jdbc/get-connection pool)

          stmt
          (jdbc/prepare conn
                        ["select $1 as num, $2 as bool" 42 true]
                        {:oids [oid/int4]})

          res
          (jdbc/execute! conn
                         [stmt 123 false]
                         {:matrix? true})]

      (is (pg/prepared-statement? stmt))
      (is (= [[:num :bool] [123 false]] res))

      (pg/close conn))))
