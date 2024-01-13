(ns pg.server
  (:import
   (java.util.concurrent Executors)
   (org.eclipse.jetty.util.thread QueuedThreadPool)
   com.zaxxer.hikari.HikariDataSource
   org.eclipse.jetty.server.Server)
  (:require
   [hikari-cp.core :as cp]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as prepare]
   [next.jdbc.result-set :as rs]
   [pg.pool :as pool]
   [pg.client :as pg]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.adapter.jetty :as jetty]))


(def USER "test")
(def PORT 15432)

(def POOL_CONN_MIN 4)
(def POOL_CONN_MAX 64)

(def pg-config
  {:host "127.0.0.1"
   :port PORT
   :user USER
   :password USER
   :database USER
   :binary-encode? true
   :binary-decode? true
   :log-level :info
   :pool-min-size POOL_CONN_MIN
   :pool-max-size POOL_CONN_MAX
   :pool-ms-lifetime 100000
   :pool-log-level :info})


(def jdbc-config
  {:dbtype "postgres"
   :port PORT
   :dbname USER
   :user USER
   :password USER})


(def cp-options
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       POOL_CONN_MIN
   :maximum-pool-size  POOL_CONN_MAX
   :pool-name          "jetty-pool"
   :adapter            "postgresql"
   :username           USER
   :password           USER
   :database-name      USER
   :server-name        "127.0.0.1"
   :port-number        PORT
   :register-mbeans    false})


(def QUERY_SELECT_RANDOM_COMPLEX
  "
select

  x::int4                  as int4,
  x::int8                  as int8,
  x::numeric               as numeric,
  x::text || 'foobar'      as line,
  x > 100500               as bool,
  null                     as nil

from
  generate_series(1,500) as s(x)

")


(def JETTY {:port 18080
            :join? true})


(defn make-pg-handler [pool]
  (fn handler [request]
    (let [data
          (pool/with-connection [conn pool]
            (pg/query conn QUERY_SELECT_RANDOM_COMPLEX))]
      {:status 200
       :body data})))


(defn make-jdbc-handler [^HikariDataSource datasource]
  (fn handler [request]
    (let [data
          (with-open [conn
                      (jdbc/get-connection datasource)]
            (jdbc/execute! conn [QUERY_SELECT_RANDOM_COMPLEX]))]
      {:status 200
       :body data})))


(defn -main-pg [& _]
  (pool/with-pool [pool pg-config]
    (let [handler
          (make-pg-handler pool)]
      (jetty/run-jetty
       (-> handler
           (wrap-json-response))
       JETTY))))


(defn -main-jdbc [& _]
  (with-open [^HikariDataSource datasource
              (cp/make-datasource cp-options)]
    (let [handler
          (make-jdbc-handler datasource)]
      (jetty/run-jetty
       (-> handler
           (wrap-json-response))
       JETTY))))


(defn -main [& [type]]
  (case type
    "pg" (-main-pg)
    "jdbc" (-main-jdbc)))
