(ns pg.concurrency-test
  (:require
   [clojure.data.csv :as csv]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.integration :refer [*CONFIG*
                           P15]]
   [pg.oid :as oid]
   [pg.pool :as pool]))


(def CONFIG
  (assoc *CONFIG* :port P15))


(deftest test-query-sleep

  (pg/with-connection [conn CONFIG]

    (let [time1
          (System/currentTimeMillis)

          f1
          (future
            (pg/query conn "select 1 as x; select pg_sleep(3); select 2 as x"))

          f2
          (future
            (pg/query conn "select pg_sleep(3); select 3 as x; select 4 as x"))

          f3
          (future
            (pg/query conn "select 5 as x; select 6 as x; select pg_sleep(3); "))

          res1 @f1
          res2 @f2
          res3 @f3

          time2
          (System/currentTimeMillis)

          diff
          (- time2 time1)]

      (is (= [[{:x 1}] [{:pg_sleep ""}] [{:x 2}]]
             res1))

      (is (= [[{:pg_sleep ""}] [{:x 3}] [{:x 4}]]
             res2))

      (is (= [[{:x 5}] [{:x 6}] [{:pg_sleep ""}]]
             res3))

      (is (< 9000 diff 9500)))))


(deftest test-execute-sleep

  (pg/with-connection [conn CONFIG]

    (let [time1
          (System/currentTimeMillis)

          f1
          (future
            (pg/execute conn "select pg_sleep(1) as A"))

          f2
          (future
            (pg/execute conn "select pg_sleep(2) as B"))

          f3
          (future
            (pg/execute conn "select pg_sleep(3) as C"))

          res1 @f1
          res2 @f2
          res3 @f3

          time2
          (System/currentTimeMillis)

          diff
          (- time2 time1)]

      (is (= [{:a ""}] res1))
      (is (= [{:b ""}] res2))
      (is (= [{:c ""}] res3))

      (is (< 6000 diff 6500)))))


(deftest test-prepare-sleep

  (pg/with-connection [conn CONFIG]

    (let [time1
          (System/currentTimeMillis)

          f1
          (future
            (pg/prepare conn "select pg_sleep($1) as A"))

          f2
          (future
            (pg/prepare conn "select pg_sleep($1) as B"))

          f3
          (future
            (pg/prepare conn "select pg_sleep($1) as C"))

          stmt1 @f1
          stmt2 @f2
          stmt3 @f3

          f1
          (future
            (pg/execute-statement conn stmt1 {:params [1.0]}))

          f2
          (future
            (pg/execute-statement conn stmt2 {:params [2.0]}))

          f3
          (future
            (pg/execute-statement conn stmt3 {:params [3.0]}))

          res1 @f1
          res2 @f2
          res3 @f3

          time2
          (System/currentTimeMillis)

          diff
          (- time2 time1)]

      (is (= [{:a ""}] res1))
      (is (= [{:b ""}] res2))
      (is (= [{:c ""}] res3))

      (is (< 6000 diff 6500)))))


(deftest test-transaction-pipeline

  (pg/with-connection [conn CONFIG]

    (let [f1
          (future
            (pg/with-tx [conn]
              (pg/query conn "select pg_sleep(1) as A")))

          f2
          (future
            (pg/with-tx [conn]
              (pg/query conn "select pg_sleep(2) as B")))

          res1 @f1
          res2 @f2]
      )
    )
  )
