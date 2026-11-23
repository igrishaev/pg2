(ns pg.ssl-test
  (:require
   [clojure.test :refer [is
                         report
                         deftest
                         testing
                         use-fixtures]]
   [pg.core :as pg]
   [pg.integration :refer [*CONFIG*]]
   [pg.ssl :as ssl]))


(defn fix-if-ssl-set [t]
  (when-let [ssl-port
             (some-> "PG_SSL_PORT"
                     System/getenv
                     Integer/parseInt)]
    (binding [*CONFIG*
              (assoc *CONFIG*
                     :port ssl-port)]
      (t))))


(use-fixtures :each fix-if-ssl-set)


(deftest test-ssl-ok

  (let [ssl-context
        (ssl/context
         "../certs/client.key"
         "../certs/client.crt"
         "../certs/root.crt")

        config
        (merge *CONFIG*
               {:host "localhost"
                :ssl-context ssl-context})]

    (with-open [conn
                (pg/connect config)]

      (is (pg/is-ssl? conn))
      (is (= [{:one 1}] (pg/query conn "select 1 as one"))))))


(deftest test-ssl-connect-timeout-not-a-read-timeout

  (let [ssl-context
        (ssl/context
         "../certs/client.key"
         "../certs/client.crt"
         "../certs/root.crt")

        config
        (merge *CONFIG*
               {:host "localhost"
                :ssl-context ssl-context
                :connect-timeout 200
                :so-timeout 0})]

    (with-open [conn
                (pg/connect config)]

      (is (pg/is-ssl? conn))
      (is (= 1 (count (pg/query conn "select pg_sleep(1)"))))
      (is (not (pg/closed? conn))))))


(deftest test-ssl-await-notifications

  (let [ssl-context
        (ssl/context
         "../certs/client.key"
         "../certs/client.crt"
         "../certs/root.crt")

        config
        (merge *CONFIG*
               {:host "localhost"
                :ssl-context ssl-context
                :so-timeout 0})

        channel
        (str (gensym "channel"))]

    (with-open [conn1 (pg/connect config)
                conn2 (pg/connect config)]

      (is (pg/is-ssl? conn2))
      (pg/listen conn2 channel)

      (testing "a timeout leaves the TLS connection usable"
        (is (= 0 (pg/await-notifications conn2 300)))
        (is (not (pg/closed? conn2)))
        (is (= [{:one 1}] (pg/query conn2 "select 1 as one"))))

      (testing "a notification ends the wait early"
        (let [sender (future
                       (Thread/sleep 300)
                       (pg/notify conn1 channel "over tls"))
              t0 (System/nanoTime)]
          (is (= 1 (pg/await-notifications conn2 10000)))
          @sender
          (is (< (quot (- (System/nanoTime) t0) 1000000) 5000))
          (is (= ["over tls"] (mapv :message (pg/drain-notifications conn2))))))

      (testing "repeated timeouts and wakeups"
        (dotimes [i 3]
          (is (= 0 (pg/await-notifications conn2 100)))
          (pg/notify conn1 channel (str i))
          (is (= 1 (pg/await-notifications conn2 10000))))
        (is (= ["0" "1" "2"] (mapv :message (pg/drain-notifications conn2))))
        (is (= [{:one 1}] (pg/query conn2 "select 1 as one")))))))
