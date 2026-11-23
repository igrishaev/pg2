(ns pg.unix-socket-test
  "
  Tests for Unix domain socket connections. They only run when
  PG_UNIX_SOCKET_PATH points to a Postgres socket, e.g.
  /tmp/.s.PGSQL.5432; PG_UNIX_SOCKET_USER, PG_UNIX_SOCKET_PASSWORD
  and PG_UNIX_SOCKET_DATABASE override the default credentials.
  "
  (:import
   (java.net SocketTimeoutException
             StandardProtocolFamily
             UnixDomainSocketAddress)
   (java.nio.channels ServerSocketChannel)
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)
   (org.pg.error PGErrorIO))
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [pg.core :as pg]
   [pg.integration :as it]))


(defn- env [k]
  (not-empty (System/getenv k)))


(defn- unix-config []
  (when-let [path (env "PG_UNIX_SOCKET_PATH")]
    {:unix-socket-path path
     :user (or (env "PG_UNIX_SOCKET_USER") it/USER)
     :password (or (env "PG_UNIX_SOCKET_PASSWORD") it/PASS)
     :database (or (env "PG_UNIX_SOCKET_DATABASE") it/DATABASE)}))


(defn- elapsed-ms [t0]
  (quot (- (System/nanoTime) t0) 1000000))


(defn- caused-by-timeout? [^Throwable e]
  (boolean (some #(instance? SocketTimeoutException %)
                 (take-while some? (iterate ex-cause e)))))


(deftest test-unix-query
  (when-let [config (unix-config)]
    (pg/with-connection [conn config]
      (is (= [{:one 1}] (pg/query conn "select 1 as one"))))))


(deftest test-unix-large-payloads
  (when-let [config (unix-config)]
    (pg/with-connection [conn config]

      (testing "a large result fills the receive buffer"
        (let [n (* 8 1024 1024)
              [{:keys [s]}] (pg/query conn (format "select repeat('x', %d) as s" n))]
          (is (= n (count s)))))

      (testing "a large parameter fills the send buffer"
        (let [s (str/join (repeat (* 8 1024 1024) "y"))
              [{:keys [len]}] (pg/execute conn "select length($1) as len" {:params [s]})]
          (is (= (count s) len))))

      (testing "many rows"
        (is (= 100000
               (count (pg/query conn "select x from generate_series(1, 100000) as x"))))))))


(deftest test-unix-so-timeout

  (when-let [config (unix-config)]

    (testing "no read timeout by default"
      (pg/with-connection [conn config]
        (is (= 1 (count (pg/query conn "select pg_sleep(1)"))))
        (is (not (pg/closed? conn)))))

    (testing "an explicit so-timeout applies to reads"
      (pg/with-connection [conn (assoc config :so-timeout 300)]
        (is (= [{:one 1}] (pg/query conn "select 1 as one")))
        (let [t0 (System/nanoTime)]
          (try
            (pg/query conn "select pg_sleep(2)")
            (is false "should have timed out")
            (catch PGErrorIO e
              (is (caused-by-timeout? e) (str "unexpected error: " e))))
          (is (< (elapsed-ms t0) 1500)))
        (is (pg/closed? conn))))

    (testing "connect-timeout is not a read timeout"
      (pg/with-connection [conn (assoc config :connect-timeout 200 :so-timeout 0)]
        (is (= 1 (count (pg/query conn "select pg_sleep(1)"))))
        (is (not (pg/closed? conn)))))))


(deftest test-unix-connect-timeout-silent-server

  ;; Only needs a writable temp dir, not a running Postgres.
  (let [dir (Files/createTempDirectory "pg2-unix" (make-array FileAttribute 0))
        path (.resolve dir ".s.PGSQL.5432")]
    (with-open [server (ServerSocketChannel/open StandardProtocolFamily/UNIX)]
      (.bind server (UnixDomainSocketAddress/of path))
      (let [accepted (atom [])
            acceptor (future
                       (try
                         (while true
                           (swap! accepted conj (.accept server)))
                         (catch Exception _)))
            t0 (System/nanoTime)
            e (try
                (pg/connect {:unix-socket-path (str path)
                             :user it/USER
                             :password it/PASS
                             :database it/DATABASE
                             :connect-timeout 300})
                nil
                (catch Exception e e))]
        (try
          (is (some? e))
          (is (caused-by-timeout? e) (str "unexpected error: " e))
          (is (< (elapsed-ms t0) 2000))
          (finally
            (future-cancel acceptor)
            (run! #(.close ^java.io.Closeable %) @accepted)))))
    (Files/deleteIfExists path)
    (Files/deleteIfExists dir)))


(deftest test-unix-interrupt

  (when-let [config (unix-config)]
    (pg/with-connection [conn config]
      (let [t0 (System/nanoTime)
            f (future (pg/query conn "select pg_sleep(5)"))]
        (Thread/sleep 300)
        (future-cancel f)
        (is (thrown? Exception @f))
        (is (< (elapsed-ms t0) 2000))
        ;; Wait for the cancelled thread to give up the connection.
        (pg/close conn)
        (is (pg/closed? conn))))))


(deftest test-unix-poll-notifications

  (when-let [config (unix-config)]
    (let [channel (str (gensym "channel"))]
      (pg/with-connection [conn1 config]
        (pg/with-connection [conn2 config]
          (pg/listen conn2 channel)
          (pg/notify conn1 channel "1")
          (pg/notify conn1 channel "2")
          (let [t0 (System/nanoTime)]
            (loop [total 0]
              (when (and (< total 2) (< (elapsed-ms t0) 5000))
                (recur (+ total (long (pg/poll-notifications conn2))))))
            (is (= ["1" "2"] (mapv :message (pg/drain-notifications conn2)))))
          (is (= [{:one 1}] (pg/query conn2 "select 1 as one"))))))))


(defn- poll-until-closed
  "Poll until the connection is closed (or 5s pass); return
  the messages of the errors poll-notifications threw."
  [conn]
  (let [t0 (System/nanoTime)]
    (loop [errors []]
      (if (or (pg/closed? conn) (> (elapsed-ms t0) 5000))
        errors
        (let [e (try
                  (pg/poll-notifications conn)
                  nil
                  (catch Exception e e))]
          (Thread/sleep 50)
          (recur (cond-> errors e (conj (str (ex-message e))))))))))


(deftest test-unix-poll-notifications-server-closed

  (when-let [config (unix-config)]
    (pg/with-connection [admin config]
      (let [conn (pg/connect config)]
        (pg/listen conn (str (gensym "channel")))
        (pg/execute admin "select pg_terminate_backend($1)" {:params [(pg/pid conn)]})
        ;; Depending on timing, the FATAL error and the end of stream
        ;; arrive in one poll or in two.
        (let [errors (poll-until-closed conn)]
          (is (pg/closed? conn) "polling should notice the server has gone")
          (is (some #(re-find #"has closed the connection" %) errors)
              (str "errors: " errors)))))))


(deftest test-unix-await-notifications

  (when-let [config (unix-config)]
    (let [channel (str (gensym "channel"))]
      (pg/with-connection [conn1 config]
        (pg/with-connection [conn2 (assoc config :so-timeout 0)]
          (pg/listen conn2 channel)

          (testing "returns 0 after the timeout; the connection survives"
            (let [t0 (System/nanoTime)]
              (is (= 0 (pg/await-notifications conn2 300)))
              (is (<= 250 (elapsed-ms t0) 2000)))
            (is (not (pg/closed? conn2)))
            (is (= [{:one 1}] (pg/query conn2 "select 1 as one"))))

          (testing "so-timeout is restored afterwards"
            (is (= 1 (count (pg/query conn2 "select pg_sleep(1)"))))
            (is (not (pg/closed? conn2))))

          (testing "a notification ends the wait early"
            (doseq [timeout-ms [10000 0]]
              (let [sender (future
                             (Thread/sleep 300)
                             (pg/notify conn1 channel (str timeout-ms)))
                    t0 (System/nanoTime)]
                (is (= 1 (pg/await-notifications conn2 timeout-ms)))
                @sender
                (is (< (elapsed-ms t0) 5000))
                (is (= [(str timeout-ms)]
                       (mapv :message (pg/drain-notifications conn2))))))))))))
