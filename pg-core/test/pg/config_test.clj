(ns pg.config-test
  (:import org.pg.Config)
  (:require
   [clojure.test :refer [deftest is testing]]
   [pg.core :as pg]))

(set! *warn-on-reflection* true)

(defn config->map
  [^Config config]
  {:database (.database config)
   :user (.user config)
   :password (.password config)
   :port (.port config)
   :host (.host config)})

(defn record->map [r]
  (into {} (for [^java.lang.reflect.RecordComponent c (seq (.getRecordComponents (class r)))]
             [(keyword (.getName c))
              (.invoke (.getAccessor c) r nil)])))

(deftest test-config-minimal
  (let [^Config config (pg/->config {:user "testuser" :database "testdb"})]
    (is (= {:database "testdb"
            :user "testuser"
            :host "127.0.0.1"
            :port 5432
            :password ""}
           (config->map config)))))

(deftest test-config-connection-uri
  #_
  (is (= {:database "test"
          :user "fred"
          :host "localhost"
          :port 5432
          :password "secret"}
         (config->map (pg/->config {:connection-uri "jdbc:postgresql://fred:secret@localhost/test?ssl=true"}))))


  (testing "parameters"
    (is (= {:SOKeepAlive false
            :SOReceiveBufSize 999
            :SOSendBufSize 888
            :SOTCPnoDelay false,
            :binaryDecode false
            :binaryEncode true
            :cancelTimeoutMs 4321
            :inStreamBufSize 77
            :outStreamBufSize 45
            :password ""
            :poolMinSize 3
            :poolMaxSize 4
            :poolBorrowConnTimeoutMs 449
            :poolExpireThresholdMs 3322
            :protocolVersion 42
            :readOnly true
            :useSSL true
            :user "fred"
            :pgParams {"application_name" "pg2"
                       "client_encoding" "UTF8"
                       "default_transaction_read_only" "on"
                       "options" "-c statement_timeout=99"}}
           (select-keys
            (record->map
             (pg/->config {:connection-uri (str "jdbc:postgresql://unused@localhost/"
                                                "test?user=fred"
                                                "&binary-encode=true"
                                                "&read-only=true"
                                                "&so-keep-alive=no"
                                                "&so-tcp-no-delay=off"
                                                "&cancel-timeout-ms=4321"
                                                "&protocol-version=42"
                                                "&pool-min-size=3"
                                                "&pool-max-size=4"
                                                "&pool-expire-threshold-ms=3322"
                                                "&pool-borrow-conn-timeout-ms=449"
                                                "&ssl=1"
                                                "&options=-c%20statement_timeout=99"
                                                "&so-recv-buf-size=999"
                                                "&so-send-buf-size=888"
                                                "&so-timeout=212"
                                                "&in-stream-buf-size=77"
                                                "&out-stream-buf-size=45")}))
            [:user :password
             :SOKeepAlive
             :SOReceiveBufSize
             :SOSendBufSize
             :SOTCPnoDelay
             :binaryDecode
             :binaryEncode
             :cancelTimeoutMs
             :inStreamBufSize
             :outStreamBufSize
             :poolBorrowConnTimeoutMs
             :poolExpireThresholdMs
             :poolMaxSize
             :poolMinSize
             :protocolVersion
             :readOnly
             :useSSL
             :pgParams])))))

(deftest test-config-connection-uri-precedence
  (testing "prefer direct map options, then PG2 named params, then JDBC params"
    (is (= {:cancelTimeoutMs 777
            :password ""
            :protocolVersion 1
            :user "user"}
           (select-keys
            (record->map
             (pg/->config {:protocol-version 1
                           :user "user"
                           :connection-uri (str "jdbc:postgresql://unused@localhost/"
                                                "test?user=notthisuser"
                                                "&protocol-version=2"
                                                "&protocolVersion=3"
                                                "&cancel-timeout-ms=777"
                                                "&cancelSignalTimeout=888")}))
            [:user :password :cancelTimeoutMs :protocolVersion])))))

(deftest test-config-connection-uri-jdbc-compat
  (testing "multiple ways to specify user/password"
    (let [expected {:database "test"
                    :user "fred"
                    :host "localhost"
                    :port 5432
                    :password "secret"}]
      (is (= expected
             (config->map (pg/->config {:connection-uri "jdbc:postgresql://fred@localhost/test?password=secret&ssl=true"}))))
      (is (= expected
             (config->map (pg/->config {:connection-uri "jdbc:postgresql://localhost/test?user=fred&password=secret&ssl=true"}))))
      (is (= expected
             (config->map (pg/->config {:connection-uri "jdbc:postgresql://fred:secret@localhost/test?ssl=true"}))))))
  (testing "scheme differences"
    (let [expected {:database "test"
                    :user "fred"
                    :host "localhost"
                    :port 5432
                    :password ""}]
      (is (= expected
             (config->map (pg/->config {:connection-uri "postgresql://localhost/test?user=fred"}))))
      (is (= expected
             (config->map (pg/->config {:connection-uri "jdbc:postgresql://localhost/test?user=fred"}))))
      (is (= expected
             (config->map (pg/->config {:connection-uri "postgres://localhost/test?user=fred"}))))))
  (testing "no password"
    (let [expected {:database "test"
                    :user "fred"
                    :host "localhost"
                    :port 5432
                    :password ""}]
      (is (= expected
             (config->map (pg/->config {:connection-uri "jdbc:postgresql://fred@localhost/test"}))))
      (is (= expected
             (config->map (pg/->config {:connection-uri "jdbc:postgresql://localhost/test?user=fred"}))))))
  (testing "other parameters"
    (is (= {:SOKeepAlive false
            :SOTCPnoDelay false,
            :binaryDecode true
            :binaryEncode true
            :cancelTimeoutMs 4321
            :password ""
            :protocolVersion 42
            :readOnly true
            :useSSL true
            :user "fred"
            :pgParams {"application_name" "foo"
                       "client_encoding" "UTF8"
                       "default_transaction_read_only" "on"}}
           (select-keys
            (record->map
             (pg/->config {:connection-uri "jdbc:postgresql://localhost/test?user=fred&binaryTransfer=true&readOnly=true&tcpKeepAlive=false&tcpNoDelay=false&cancelSignalTimeout=4321&ApplicationName=foo&protocolVersion=42&ssl=on"}))
            [:user :password :SOKeepAlive :SOTCPnoDelay :binaryEncode :binaryDecode
             :cancelTimeoutMs :protocolVersion :readOnly :useSSL :pgParams])))))
