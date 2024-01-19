(ns pg.bench
  (:import
   java.util.HashMap
   java.util.ArrayList
   java.sql.DriverManager
   java.sql.ResultSet
   java.sql.Statement
   com.zaxxer.hikari.HikariDataSource
   java.io.ByteArrayOutputStream
   java.io.InputStream
   java.io.OutputStream
   java.sql.PreparedStatement
   java.time.LocalDateTime
   java.util.concurrent.ExecutorService
   java.util.concurrent.Executors
   org.pg.ConnConfig$Builder
   org.pg.Connection
   org.postgresql.copy.CopyManager
   org.postgresql.util.PGobject)
  (:use criterium.core)
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [hikari-cp.core :as cp]
   [jsonista.core :as json]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as prepare]
   [next.jdbc.result-set :as rs]
   [pg.json]
   [pg.core :as pg]
   [pg.oid :as oid]
   [pg.pool :as pool]))

(set! *warn-on-reflection* true)

(def USER "test")
(def PORT 15432)


;;
;; next.JDBC stuff
;;

(def mapper
  (json/object-mapper {:decode-key-fn keyword}))


(def ->json json/write-value-as-string)


(defn <-json [x]
  (json/read-value x mapper))


(defn ->pgobject [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))


(defn <-pgobject
  [^org.postgresql.util.PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (<-json value) {:pgtype type}))
      value)))


(extend-protocol prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))

  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))


(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v)))


(def POOL_CONN_MIN 4)
(def POOL_CONN_MAX 8)


(def HOST "127.0.0.1")

(def JDBC-URL
  (format "jdbc:postgresql://%s:%s/%s" HOST PORT USER))


(def pg-config
  {:host HOST
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
   :pool-name          "bench-pool"
   :adapter            "postgresql"
   :username           USER
   :password           USER
   :database-name      USER
   :server-name        HOST
   :port-number        PORT
   :register-mbeans    false})


(def QUERY_SELECT_JSON
  "select row_to_json(row(1, random(), 2, random()))
   from generate_series(1,50000);")

(def QUERY_SELECT_RANDOM_SIMPLE
  "
select x
from
  generate_series(1,50000) as s(x)
")


(def QUERY_SELECT_RANDOM_COMPLEX
  "
select

  x::int4                  as int4,
  x::int8                  as int8,
  x::numeric               as numeric,
  x::text || 'foobar'      as line,
  x > 100500               as bool,
  now()                    as ts,
  now()::date              as date,
  now()::time              as time,
  null                     as nil

from
  generate_series(1,50000) as s(x)

")

(def QUERY_INSERT_PG
  "insert into aaa(id, name, created_at) values ($1, $2, $3)")

(def QUERY_INSERT_JDBC
  "insert into aaa(id, name, created_at) values (?, ?, ?)")

(def QUERY_TABLE
  "create table if not exists aaa (id integer not null, name text not null, created_at timestamp not null)")

(def QUERY_IN_STREAM
  "copy aaa (id, name, created_at) from STDIN WITH (FORMAT CSV)")

(def QUERY_IN_STREAM_BIN
  "copy aaa (id, name, created_at) from STDIN WITH (FORMAT BINARY)")

(def QUERY_OUT_STREAM
  "copy (select random() from generate_series(0, 999999)) TO STDIN WITH (FORMAT CSV)")

(def SAMPLE_CSV
  "sample.csv")


(defn generate-csv []
  (let [rows
        (for [x (range 0 1000000)]
          [x
           (str "name" x)
           (LocalDateTime/now)])]
    (with-open [writer (-> SAMPLE_CSV
                           io/file
                           io/writer)]
      (csv/write-csv writer rows))))


(def GEN_LIMIT 100000)

(defn generate-rows []
  (for [x (range GEN_LIMIT)]
    [x
     (str "name" x)
     (LocalDateTime/now)]))


(defn generate-maps []
  (for [x (range GEN_LIMIT)]
    {:id x
     :name (str "name" x)
     :created_at (LocalDateTime/now)}))


(defmacro with-title [line & body]
  `(let [line# ~line
         border# (.repeat "-" (count line#))]
     (println border#)
     (println line#)
     (println border#)
     ~@body))


(defmacro with-virt-exe [[n] & body]
  `(with-open [exe#
               (Executors/newVirtualThreadPerTaskExecutor)]
     (doseq [_# (range 0 ~n)]
       (.submit exe# (reify Callable
                       (call [_]
                         ~@body))))))


(defn process-row [row]
  (assoc row :foo 42))


(defn fold-row [acc {:keys [x] :as row}]
  (assoc acc x (assoc row :extra 42)))


(defn -main [& args]

  (with-title "generating CSV"
    (generate-csv))

  (pg/with-connection [conn pg-config]
    (pg/execute conn QUERY_TABLE))

  (with-title "next.JDBC reduce run!"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]
      (quick-bench
       (let [result
             (jdbc/plan conn [QUERY_SELECT_RANDOM_SIMPLE])]
         (run! process-row result)))))

  (with-title "pg reduce run!"
    (pg/with-connection [conn pg-config]
      (pg/with-statement [stmt
                          conn
                          QUERY_SELECT_RANDOM_SIMPLE]
        (quick-bench
         (pg/execute-statement conn
                               stmt
                               {:run process-row})))))

  (with-title "pg reduce map"
    (pg/with-connection [conn pg-config]
      (pg/with-statement [stmt
                          conn
                          QUERY_SELECT_RANDOM_SIMPLE]
        (quick-bench
         (pg/execute-statement conn
                               stmt
                               {:fold fold-row
                                :init {}})))))

  (with-title "next.JDBC reduce map"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]

      (quick-bench
       (let [result
             (jdbc/plan conn [QUERY_SELECT_RANDOM_SIMPLE])]
         (reduce fold-row
                 {}
                 result)))))

  (with-title "pure JDBC simple select"
    (let [^java.sql.Connection conn
          (DriverManager/getConnection JDBC-URL USER USER)
          ^PreparedStatement stmt
          (.prepareStatement conn QUERY_SELECT_RANDOM_SIMPLE)]
      (quick-bench
       (let [^ResultSet rs (.executeQuery stmt)
             ^ArrayList l (new ArrayList)]
         (while (.next rs)
           (let [^HashMap m (new HashMap)]
             (.put m "x" (.getString rs "x"))
             (.add l m)))))))

  (with-title "next.JDBC simple value select"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]

      (quick-bench
       (jdbc/execute! conn
                      [QUERY_SELECT_RANDOM_SIMPLE]
                      {:as rs/as-unqualified-maps}))))

  (with-title "pg complex simple select"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/execute conn
                   QUERY_SELECT_RANDOM_SIMPLE))))

  (with-title "next.JDBC complex value select"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]

      (quick-bench
       (jdbc/execute! conn
                      [QUERY_SELECT_RANDOM_COMPLEX]
                      {:as rs/as-unqualified-maps}))))

  (with-title "pg complex value select"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/execute conn
                   QUERY_SELECT_RANDOM_COMPLEX))))

  (with-title "next.JDBC random JSON select"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]

      (quick-bench
       (jdbc/execute! conn
                      [QUERY_SELECT_JSON]
                      {:as rs/as-unqualified-maps}))))

  (with-title "pg random JSON select"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/execute conn
                   QUERY_SELECT_JSON))))

  (with-title "pg insert values in TRANSACTION"
    (pg/with-connection [conn pg-config]
      (pg/with-statement [stmt
                          conn
                          QUERY_INSERT_PG]
        (quick-bench
         (let [x (rand-int 10000)]
           (pg/with-tx [conn]
             (pg/execute-statement conn
                                   stmt
                                   {:params [x,
                                             (format "name%s" x)
                                             (LocalDateTime/now)]})))))))

  (with-title "next.JDBC insert values in TRANSACTION"

    (with-open [conn (jdbc/get-connection
                      jdbc-config)]

      (quick-bench
       (let [x (rand-int 10000)]
         (jdbc/with-transaction [tx conn]
           (jdbc/execute! tx
                          [QUERY_INSERT_JDBC
                           x,
                           (format "name%s" x)
                           (LocalDateTime/now)]))))))

  (with-title "pg insert values"
    (pg/with-connection [conn pg-config]
      (pg/with-statement [stmt
                          conn
                          QUERY_INSERT_PG]
        (quick-bench
         (let [x (rand-int 10000)]
           (pg/execute-statement conn
                                 stmt
                                 {:params [x,
                                           (format "name%s" x)
                                           (LocalDateTime/now)]}))))))

  (with-title "next.JDBC insert values"

    (with-open [conn (jdbc/get-connection
                      jdbc-config)]

      (quick-bench
       (let [x (rand-int 10000)]
         (jdbc/execute! conn
                        [QUERY_INSERT_JDBC
                         x,
                         (format "name%s" x)
                         (LocalDateTime/now)])))))

  (with-title "JDBC COPY in from a stream"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]
      (quick-bench
       (let [copy
             (new CopyManager conn)]

         (.copyIn copy
                  ^String QUERY_IN_STREAM
                  ^InputStream (-> SAMPLE_CSV io/file io/input-stream))))))

  (with-title "PG COPY in from a stream"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-in conn
                   QUERY_IN_STREAM
                   (-> SAMPLE_CSV io/file io/input-stream)))))

  (with-title "PG COPY in from rows BIN"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-in-rows conn
                        QUERY_IN_STREAM_BIN
                        (generate-rows)
                        {:copy-bin? true
                         :oids [oid/int4 oid/text oid/timestamp]}))))

  (with-title "PG COPY in from rows CSV"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-in-rows conn
                        QUERY_IN_STREAM
                        (generate-rows)))))

  (with-title "PG COPY in from maps BIN"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-in-maps conn
                        QUERY_IN_STREAM_BIN
                        (generate-maps)
                        [:id :name :created_at]
                        {:copy-bin? true
                         :oids [oid/int4 oid/text oid/timestamp]}))))

  (with-title "PG COPY in from maps CSV"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-in-maps conn
                        QUERY_IN_STREAM
                        (generate-maps)
                        [:id :name :created_at]))))

  (with-title "PG COPY out"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-out conn
                    QUERY_OUT_STREAM
                    (OutputStream/nullOutputStream)))))

  (with-title "JDBC COPY out"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]
      (quick-bench
       (let [copy
             (new CopyManager conn)]

         (.copyOut copy
                   ^String QUERY_OUT_STREAM
                   ^OutputStream (OutputStream/nullOutputStream))))))


  (with-title "PG virtual threads"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (with-virt-exe [8]
         (pg/execute conn QUERY_SELECT_JSON)))))

  (with-title "JDBC virtual threads"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]
      (quick-bench
       (with-virt-exe [8]
         (jdbc/execute! conn
                        [QUERY_SELECT_JSON]
                        {:as rs/as-unqualified-maps})))))

  (with-title "JDBC pool"
    (with-open [^HikariDataSource datasource
                (cp/make-datasource cp-options)]
      (quick-bench
       (with-virt-exe [8]
         (with-open [conn
                     (jdbc/get-connection datasource)]
           (jdbc/execute! conn [QUERY_SELECT_JSON]))))))

  (with-title "PG pool"
    (pool/with-pool [pool pg-config]
      (quick-bench
       (with-virt-exe [8]
         (pool/with-connection [conn pool]
           (pg/execute conn QUERY_SELECT_JSON))))))

  )
