(ns pg.ssl-test
  (:require
   [clojure.test :refer [is
                         report
                         deftest
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
