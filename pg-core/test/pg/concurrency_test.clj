(ns pg.concurrency-test
  (:import
   java.io.ByteArrayOutputStream)
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
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

      (is (< 6000 diff 6100)))))


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
            (pg/execute-statement conn stmt2 {:params [2]}))

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

      (is (< 6000 diff 6100)))))


(deftest test-transaction-pipeline

  (pg/with-connection [conn CONFIG]

    (let [time1
          (System/currentTimeMillis)

          f1
          (future
            (pg/with-tx [conn]
              (pg/execute conn
                          "select pg_sleep(1) as A"
                          {:first? true})))

          f2
          (future
            (pg/with-tx [conn]
              (pg/execute conn
                          "select pg_sleep(2) as B"
                          {:first? true})))

          res1 @f1
          res2 @f2

          time2
          (System/currentTimeMillis)

          diff
          (- time2 time1)]

      (is (= {:a ""} res1))
      (is (= {:b ""} res2))

      (is (< 3000 diff 3100)))))


(deftest test-copy-out-parallel

  (pg/with-connection [conn CONFIG]

    (let [time1
          (System/currentTimeMillis)

          sql1
          "select pg_sleep(2); copy (select s.x as x, s.x * s.x as square from generate_series(    1, 10000) as s(x)) TO STDOUT WITH (FORMAT CSV)"

          sql2
          "select pg_sleep(1); copy (select s.x as x, s.x * s.x as square from generate_series(10001, 20000) as s(x)) TO STDOUT WITH (FORMAT CSV)"

          out
          (new ByteArrayOutputStream)

          f1
          (future
            (pg/copy-out conn sql1 out))

          _
          (Thread/sleep 100)

          f2
          (future
            (pg/copy-out conn sql2 out))

          [_ res1] @f1
          [_ res2] @f2

          time2
          (System/currentTimeMillis)

          diff
          (- time2 time1)

          rows
          (with-open [reader (-> out
                                 (.toByteArray)
                                 (io/input-stream)
                                 (io/reader))]
            (vec (csv/read-csv reader)))]

      (is (< 3000 diff 3100))

      (is (= {:copied 10000} res1))
      (is (= {:copied 10000} res2))

      (is (= (count rows) 20000))

      (is (= [     "1"        "1"] (first rows)))
      (is (= ["20000" "400000000"] (last rows))))))


(deftest test-copy-in-parallel

  (pg/with-connection [conn CONFIG]

    (pg/query conn "create temp table foo (x bigint)")

    (let [time1
          (System/currentTimeMillis)

          sql1
          "select pg_sleep(1.5); copy foo (x) from STDIN WITH (FORMAT CSV)"

          sql2
          "select pg_sleep(0.5); copy foo (x) from STDIN WITH (FORMAT CSV)"

          limit
          9999

          rows1
          (for [x (range 0 limit)]
            [x])

          rows2
          (for [x (range limit (* 2 limit))]
            [x])

          f1
          (future
            (pg/copy-in-rows conn sql1 rows1))

          _
          (Thread/sleep 100)

          f2
          (future
            (pg/copy-in-rows conn sql2 rows2))

          [_ res1] @f1
          [_ res2] @f2

          time2
          (System/currentTimeMillis)

          diff
          (- time2 time1)

          res
          (pg/query conn "select * from foo")]

      (is (< 2000 diff 2100))

      (is (= {:copied 9999} res1))
      (is (= {:copied 9999} res2))

      (is (= (count res) 19998))

      (is (= {:x 0} (first res)))
      (is (= {:x 19997} (last res))))))


;; TODO
;; tx-status
;; idel in tx tx-error ;; get-param ;; pid
;; close-statement
;; execute-statement
;; prepare
;; with connection
