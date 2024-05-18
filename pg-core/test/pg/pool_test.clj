(ns pg.pool-test
  (:import
   org.pg.error.PGError
   org.pg.error.PGErrorResponse)
  (:require
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.pool :as pool]))


(def ^:dynamic *CONFIG*
  {:host "127.0.0.1"
   :port 10130
   :user "test"
   :password "test"
   :database "test"})


(deftest test-pool-it-works
  (pool/with-pool [pool *CONFIG*]
    (pool/with-connection [conn pool]
      (let [res (pg/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))))


(deftest test-pool-basic-features
  (pool/with-pool [pool (assoc *CONFIG*
                               :pool-max-size 2)]

    (let [t1-conn-id
          (promise)

          t1-start
          (promise)

          t1-stop
          (promise)

          t2-start
          (promise)

          t2-stop
          (promise)

          f1
          (future
            (pool/with-connection [conn pool]
              (deliver t1-start true)
              (deliver t1-conn-id (pg/id conn))
              (Thread/sleep 1000)
              (pg/execute conn "select 1 as one"))
            (deliver t1-stop true))

          f2
          (future
            (pool/with-connection [conn pool]
              (deliver t2-start true)
              (Thread/sleep 2000)
              (pg/execute conn "select 2 as two"))
            (deliver t2-stop true))]

      @t1-start
      @t2-start

      (is (= 0 (pool/free-count pool)))
      (is (= 2 (pool/used-count pool)))

      (try
        (pool/with-connection [conn pool]
          (pg/execute conn "select 42"))
        (is false)
        (catch PGError e
          (is (= "The pool is exhausted: 2 out of 2 connections are in use"
                 (ex-message e)))))

      @t1-stop

      (is (= 1 (pool/free-count pool)))
      (is (= 1 (pool/used-count pool)))

      (let [res
            (pool/with-connection [conn pool]
              (is (= @t1-conn-id (pg/id conn)))
              (pg/execute conn "select 3 as three"))]
        (is (= [{:three 3}] res)))

      @t2-stop

      (is (= 2 (pool/free-count pool)))
      (is (= 0 (pool/used-count pool))))))


(deftest test-pool-lifetime

  (let [capture1
        (atom nil)

        capture2
        (atom nil)

        capture3
        (atom nil)

        config
        (assoc *CONFIG*
               :pool-min-size 1
               :pool-max-size 1
               :pool-lifetime-ms 300)]

    (pool/with-pool [pool config]

      (is (= {:free 1 :used 0}
             (pool/stats pool)))

      (pool/with-connection [conn pool]
        (reset! capture1 (pg/id conn)))

      (Thread/sleep 150)

      (pool/with-connection [conn pool]
        (reset! capture2 (pg/id conn)))

      (Thread/sleep 200)

      (pool/with-connection [conn pool]
        (reset! capture3 (pg/id conn)))

      (let [uuid1 @capture1
            uuid2 @capture2
            uuid3 @capture3]

        (is (uuid? uuid1))
        (is (uuid? uuid2))
        (is (uuid? uuid3))

        (is (= uuid1 uuid2))
        (is (not= uuid2 uuid3))))))


(deftest test-pool-in-transaction-state
  (pool/with-pool [pool (assoc *CONFIG*
                               :pool-min-size 1
                               :pool-max-size 1)]

    (let [id1
          (promise)

          id2
          (promise)

          id3
          (promise)

          id4
          (promise)]

      (pool/with-connection [conn pool]
        (deliver id1 (pg/id conn)))

      (pool/with-connection [conn pool]
        (deliver id2 (pg/id conn)))

      (pool/with-connection [conn pool]
        (pg/begin conn)
        (deliver id3 (pg/id conn))
        (is (pg/in-transaction? conn)))

      (pool/with-connection [conn pool]
        (is (pg/idle? conn))
        (deliver id4 (pg/id conn)))

      (is (= @id1 @id2 @id3 @id4)))))


(deftest test-pool-in-error-state
  (pool/with-pool [pool (assoc *CONFIG*
                               :pool-min-size 1
                               :pool-max-size 1)]

    (let [id1
          (promise)

          id2
          (promise)

          id3
          (promise)]

      (pool/with-connection [conn pool]
        (deliver id1 (pg/id conn)))

      (pool/with-connection [conn pool]
        (pg/begin conn)
        (try
          (pg/execute conn "selekt 42")
          (is false)
          (catch PGErrorResponse e
            (is (pg/tx-error? conn))))
        (deliver id2 (pg/id conn)))

      (pool/with-connection [conn pool]
        (is (pg/idle? conn))
        (deliver id3 (pg/id conn)))

      (is (= @id1 @id2))
      (is (not= @id2 @id3)))))


(deftest test-pool-with-open
  (with-open [pool (pool/pool *CONFIG*)]
    (pool/with-connection [conn pool]
      (let [res (pg/execute conn "select 1 as one")]
        (is (= [{:one 1}] res))))))


(deftest test-pool-conn-terminated

  (let [id1
        (promise)

        id2
        (promise)

        id3
        (promise)

        id4
        (promise)

        id5
        (promise)

        config
        (assoc *CONFIG*
               :pool-min-size 0
               :pool-max-size 1)]

    (with-open [pool (pool/pool config)]

      (pool/with-connection [conn pool]
        (deliver id1 (pg/id conn)))

      (pool/with-connection [conn pool]
        (pg/close conn)
        (deliver id2 (pg/id conn)))

      (pool/with-connection [conn pool]
        (deliver id3 (pg/id conn))
        (let [res (pg/execute conn "select 1 as one")]
          (is (= [{:one 1}] res))))

      (pool/with-connection [conn pool]
        (deliver id4 (pg/id conn)))

      (is (= @id1 @id2))
      (is (not= @id2 @id3))
      (is (= @id3 @id4)))))


(deftest test-pool-termination

  (pool/with-pool [pool *CONFIG*]
    (pool/close pool)

    (try
      (pool/with-connection [conn pool]
        42)
      (is false)
      (catch PGError e
        (is (= "Cannot get a connection: the pool has been closed"
               (ex-message e)))))))


(deftest test-pool-string-repr
  (pool/with-pool [pool *CONFIG*]
    (let [result
          "<PG pool, min: 2, max: 8, lifetime: 900000>"]
      (is (= result (str pool)))
      (is (= result (with-out-str
                      (print pool)))))))
