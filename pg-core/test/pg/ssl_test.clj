(ns pg.ssl-test
  (:require
   [pg.core :as pg]
   [clojure.test :refer [is
                         report
                         deftest
                         use-fixtures]]
   [pg.integration :refer [*CONFIG*]]))


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
        (pg/ssl-context
         {:key-file "../certs/client.key"
          :cert-file "../certs/client.crt"
          :ca-cert-file "../certs/root.crt"})

        config
        (merge *CONFIG*
               {:use-ssl? true
                :host "localhost"
                :ssl-context ssl-context})]

    (with-open [conn
                (pg/connect config)]

      (is (pg/is-ssl? conn))
      (is (= [{:one 1}] (pg/query conn "select 1 as one"))))))
