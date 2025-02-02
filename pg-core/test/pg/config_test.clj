(ns pg.config-test
  (:import org.pg.Config)
  (:require
   [clojure.test :refer [deftest is testing]]
   [pg.config :as config]
   [pg.connection-uri :as uri]
   [pg.core :as pg]))

(set! *warn-on-reflection* true)

(def FIELDS_MIN
  [:database
   :user
   :password
   :port
   :host])

(defn record->map
  "
  Turn a Java record into a Clojure map via reflection.
  Fields, if passed, act like `select-keys` to truncate
  the final map.
  "
  ([r]
   (into {} (for [^java.lang.reflect.RecordComponent c
                  (seq (.getRecordComponents (class r)))]
              [(keyword (.getName c))
               (.invoke (.getAccessor c) r nil)])))
  ([r fields]
   (-> r
       record->map
       (select-keys fields))))

(defn options->map
  ([options]
   (options->map options FIELDS_MIN))

  ([options fields]
   (-> options config/->config (record->map fields))))

(deftest test-config-minimal
  (is (= {:user "testuser",
          :database "testdb",
          :host "127.0.0.1",
          :port 5432,
          :password ""}
         (options->map {:user "testuser" :database "testdb"}))))

(deftest test-config-connection-uri
  (is (= {:database "test"
          :user "fred"
          :host "localhost"
          :port 5432
          :password "secret"}
         (-> {:connection-uri "jdbc:postgresql://fred:secret@localhost/test?ssl=true"}
             (options->map))))

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
                       "default_transaction_read_only" "on"}}

           (-> {:connection-uri (str "jdbc:postgresql://unused@localhost/"
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
                                     "&so-recv-buf-size=999"
                                     "&so-send-buf-size=888"
                                     "&so-timeout=212"
                                     "&in-stream-buf-size=77"
                                     "&out-stream-buf-size=45")}
               (options->map [:user :password
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
                              :pgParams]))))))

(deftest test-config-connection-uri-precedence
  (testing "prefer direct map options, then PG2 named params, then JDBC params"
    (is (= {:cancelTimeoutMs 777
            :password ""
            :protocolVersion 1
            :user "user"}
           (-> {:protocol-version 1
                :user "user"
                :connection-uri (str "jdbc:postgresql://unused@localhost/"
                                     "test?user=notthisuser"
                                     "&protocol-version=2"
                                     "&protocolVersion=3"
                                     "&cancel-timeout-ms=777"
                                     "&cancelSignalTimeout=888")}
               (options->map [:user :password :cancelTimeoutMs :protocolVersion]))))))

(deftest test-config-connection-uri-jdbc-compat

  (testing "multiple ways to specify user/password"
    (let [expected {:database "test"
                    :user "fred"
                    :host "localhost"
                    :port 5432
                    :password "secret"}]
      (is (= expected
             (options->map {:connection-uri "jdbc:postgresql://fred@localhost/test?password=secret&ssl=true"})))
      (is (= expected
             (options->map {:connection-uri "jdbc:postgresql://localhost/test?user=fred&password=secret&ssl=true"})))
      (is (= expected
             (options->map {:connection-uri "jdbc:postgresql://fred:secret@localhost/test?ssl=true"})))))

  (testing "scheme differences"
    (let [expected {:database "test"
                    :user "fred"
                    :host "localhost"
                    :port 5432
                    :password ""}]
      (is (= expected
             (options->map {:connection-uri "postgresql://localhost/test?user=fred"})))
      (is (= expected
             (options->map {:connection-uri "jdbc:postgresql://localhost/test?user=fred"})))
      (is (= expected
             (options->map {:connection-uri "postgres://localhost/test?user=fred"})))))

  (testing "no password"
    (let [expected {:database "test"
                    :user "fred"
                    :host "localhost"
                    :port 5432
                    :password ""}]
      (is (= expected
             (options->map {:connection-uri "jdbc:postgresql://fred@localhost/test"})))
      (is (= expected
             (options->map {:connection-uri "jdbc:postgresql://localhost/test?user=fred"})))))

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
           (-> {:connection-uri (str "jdbc:postgresql://localhost/test"
                                     "?user=fred"
                                     "&binaryTransfer=true"
                                     "&readOnly=true"
                                     "&tcpKeepAlive=false"
                                     "&tcpNoDelay=false"
                                     "&cancelSignalTimeout=4321"
                                     "&ApplicationName=foo"
                                     "&protocolVersion=42"
                                     "&ssl=on")}
               (options->map [:user :password
                              :SOKeepAlive :SOTCPnoDelay
                              :binaryEncode
                              :binaryDecode
                              :cancelTimeoutMs :protocolVersion
                              :readOnly :useSSL :pgParams]))))))

(deftest test-parse-ref-fields
  (is (= {:user "fred",
          :fnNotice clojure.core/println}
         (-> {:connection-uri "postgresql://fred:secret@localhost/test?fn-notice=clojure.core/println"}
             (options->map [:user :fnNotification])))))

(deftest test-parse-nested-params
  (is (= {:user "fred",
          :pgParams {"application_name" "pg2"
                     "client_encoding" "UTF8"
                     "opt_one" "aa,bb,cc"
                     "opt_two" "100500.00"}}
         (-> {:connection-uri "postgresql://fred:secret@localhost/test?pg-params.opt_one=aa,bb,cc&pg-params.opt_two=100500.00"}
             (options->map [:user :pgParams])))))

(deftest test-weird-cases

  (try
    (-> {:connection-uri "postgresql://fred:secret@localhost/test?fn-notice=clojure.core/ASDdfg34324"}
        (options->map))
    (is false)
    (catch Exception e
      (is (= "cannot resolve a reference: clojure.core/ASDdfg34324, reason: reference not found: clojure.core/ASDdfg34324"
             (ex-message e)))))

  (try
    (-> {:connection-uri "postgresql://fred:secret@localhost/test?fn-notice=ASDdfg34324"}
        (options->map))
    (is false)
    (catch Exception e
      (is (= "cannot resolve a reference: ASDdfg34324, reason: Not a qualified symbol: ASDdfg34324"
             (ex-message e)))))

  (try
    (-> {:connection-uri "postgresql://fred:secret@localhost/test?cancel-timeout-ms=asdfad"}
        (options->map))
    (is false)
    (catch Exception e
      (is (= "cannot parse long value: asdfad, reason: For input string: \"asdfad\""
             (ex-message e)))))

  (try
    (-> {:connection-uri "postgresql://fred:secret@localhost/test?binary-encode=dunno"}
        (options->map))
    (is false)
    (catch Exception e
      (is (= "cannot parse boolean value: dunno"
             (ex-message e))))))


(deftest test-parse-query-string

  (is (= nil
         (uri/parse-query-string "")))

  (is (= nil
         (uri/parse-query-string nil)))

  (is (= nil
         (uri/parse-query-string "a")))

  (is (= {"a" "1"}
         (uri/parse-query-string "a=1")))

  (is (= nil
         (uri/parse-query-string "a=")))

  (is (= {"a" "2"}
         (uri/parse-query-string "a=1&a=2")))

  (is (= {"a" "1" "b" "2"}
         (uri/parse-query-string "a=1&b=2")))

  (is (= {"a" {"b" "1", "c" "2"}}
         (uri/parse-query-string "a.b=1&a.c=2"))))
