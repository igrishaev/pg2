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


(deftest test-pool-short-name
  (pool/with-pool [pool *CONFIG*]
    (pool/with-conn [conn pool]
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

      (pool/with-connection [conn pool]
        (pg/execute conn "select 42"))

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

      (is (= {:free 1, :used 0}
             (pool/stats pool)))

      ;; no transaction, and this a weird query won't lead
      ;; to the error state in the connection
      (pool/with-connection [conn pool]
        (try
          (pg/execute conn "selekt 42")
          (catch PGErrorResponse e))
        (deliver id1 (pg/id conn)))

      (is (= {:free 1, :used 0}
             (pool/stats pool)))

      ;; There is a transaction, so a weird query will lead
      ;; to the error state in the transaction. A connection
      ;; with TX error gets rolled back and closed by the bool.
      (pool/with-connection [conn pool]
        (pg/begin conn)
        (try
          (pg/execute conn "selekt 42")
          (is false)
          (catch PGErrorResponse e
            (is (pg/tx-error? conn))))
        (deliver id2 (pg/id conn)))

      (is (= {:free 0, :used 0}
             (pool/stats pool)))

      (pool/with-connection [conn pool]
        (deliver id3 (pg/id conn))
        (is (= {:free 0, :used 1}
               (pool/stats pool))))

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


(deftest test-pool-wrong-sizes
  (let [config
        (assoc *CONFIG*
               :pool-min-size 3
               :pool-max-size 2)]
    (try
      (pool/pool config)
      (is false)
      (catch PGError e
        (is (= "pool min size (3) must be <= pool max size (2)"
               (ex-message e)))))))


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
          (format "<PG pool %s, min: 2, max: 8, expire in: 300000>"
                  (pool/id pool))]
      (is (= result (str pool)))
      (is (= result (with-out-str
                      (print pool)))))))


(deftest test-pool-connection-expiration

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
               :pool-expire-threshold-ms 500)]

    (pool/with-pool [pool config]

      (is (= {:free 1 :used 0}
             (pool/stats pool)))

      (pool/with-connection [conn pool]
        (reset! capture1 (pg/id conn)))

      (pool/with-connection [conn pool]
        (reset! capture2 (pg/id conn)))

      (is (= {:free 1 :used 0}
             (pool/stats pool)))

      (Thread/sleep 600)

      (pool/with-connection [conn pool]
        (reset! capture3 (pg/id conn)))

      (is (= {:free 1 :used 0}
             (pool/stats pool)))

      (let [uuid1 @capture1
            uuid2 @capture2
            uuid3 @capture3]

        (is (uuid? uuid1))
        (is (uuid? uuid2))
        (is (uuid? uuid3))

        (is (= uuid1 uuid2))
        (is (not= uuid2 uuid3))))))


(deftest test-pool-replenishment

  (let [config
        (assoc *CONFIG*
               :pool-min-size 12
               :pool-max-size 16)]

    (pool/with-pool [pool config]

      (is (= {:free 12 :used 0}
             (pool/stats pool)))

      (pool/with-connection [conn pool]
        (is (= {:free 11 :used 1}
               (pool/stats pool)))

        (pool/with-connection [conn pool]
          (pg/close conn))
        (pool/with-connection [conn pool]
          (pg/close conn))
        (pool/with-connection [conn pool]
          (pg/close conn))
        (pool/with-connection [conn pool]
          (pg/close conn))
        (pool/with-connection [conn pool]
          (pg/close conn))
        (pool/with-connection [conn pool]
          (pg/close conn))

        (is (= {:free 5 :used 1}
               (pool/stats pool)))

        (pool/replenish-connections pool)

        (is (= {:free 11 :used 1}
               (pool/stats pool)))))))


(deftest test-conn-returned-in-the-background

  (let [conn1-started
        (promise)]

    (pool/with-pool [pool (assoc *CONFIG*
                                 :pool-min-size 1
                                 :pool-max-size 1
                                 :pool-borrow-conn-timeout-ms 2000)]

      (future
        (pool/with-connection [conn pool]
          (deliver conn1-started true)
          (Thread/sleep 1000)))

      @conn1-started

      (is (= {:free 0 :used 1}
             (pool/stats pool)))

      (pool/with-connection [conn pool]
        (is (some? conn)))

      (is (= {:free 1 :used 0}
             (pool/stats pool))))))


(deftest test-could-not-return-in-the-background

  (let [conn1-started
        (promise)]

    (pool/with-pool [pool (assoc *CONFIG*
                                 :pool-min-size 1
                                 :pool-max-size 1
                                 :pool-borrow-conn-timeout-ms 500)]

      (future
        (pool/with-connection [conn pool]
          (deliver conn1-started true)
          (Thread/sleep 1000)))

      @conn1-started

      (is (= {:free 0 :used 1}
             (pool/stats pool)))

      (try
        (pool/with-connection [conn pool])
        (is false)
        (catch PGError e
          (is (= (format "Pool %s is exhausted! min: 1, max: 1, free: 0, used: 1, timeout: 500"
                         (pool/id pool))
                 (ex-message e)))))

      (is (= {:free 0 :used 1}
             (pool/stats pool))))))


(deftest test-pool-exception-in-macro

  (pool/with-pool [pool *CONFIG*]

    (is (= {:free 2, :used 0}
           (pool/stats pool)))

    (future
      (pool/with-conn [conn pool]
        (/ 0 0)))

    (Thread/sleep 300)

    (is (= {:free 2, :used 0}
           (pool/stats pool)))))
