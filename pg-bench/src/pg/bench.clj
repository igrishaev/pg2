(ns pg.bench
  (:import
   java.io.ByteArrayOutputStream
   java.io.InputStream
   java.io.OutputStream
   java.sql.PreparedStatement
   java.time.LocalDateTime
   org.pg.ConnConfig$Builder
   org.pg.Connection
   org.postgresql.copy.CopyManager
   org.postgresql.util.PGobject)
  (:use criterium.core)
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [jsonista.core :as json]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as prepare]
   [next.jdbc.result-set :as rs]
   [pg.client :as pg]
   [pg.oid :as oid]))


(def USER "ivan")
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


(def pg-config
  {:host "127.0.0.1"
   :port PORT
   :user USER
   :password USER
   :database USER

   :binary-encode? true
   :binary-decode? true

   :so-recv-buf-size (int 0xFFFF)
   :so-send-buf-size (int 0xFFFF)

   })


(def jdbc-config
  {:dbtype "postgres"
   :port PORT
   :dbname USER
   :user USER
   :password USER})


(def QUERY_SELECT_JSON
  "select row_to_json(row(1, random(), 2, random()))
   from generate_series(1,50000);")

(def QUERY_SELECT_RANDOM_VAL
  "select random() as x from generate_series(1,50000)")

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


(defn -main [& args]

  (with-title "generating CSV"
    (generate-csv))

  (with-title "pg random value select"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/execute conn
                   QUERY_SELECT_RANDOM_VAL))))

  (with-title "next.JDBC random value select"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]

      (quick-bench
       (jdbc/execute! conn
                      [QUERY_SELECT_RANDOM_VAL]
                      {:as rs/as-unqualified-maps}))))

  (with-title "pg random JSON select"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/execute conn
                   QUERY_SELECT_JSON))))

  (with-title "next.JDBC random JSON select"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]

      (quick-bench
       (jdbc/execute! conn
                      [QUERY_SELECT_JSON]
                      {:as rs/as-unqualified-maps}))))

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

  (with-title "PG COPY in from a stream"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-in conn
                   QUERY_IN_STREAM
                   (-> SAMPLE_CSV io/file io/input-stream)))))

  (with-title "JDBC COPY in from a stream"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]
      (quick-bench
       (let [copy
             (new CopyManager conn)]

         (.copyIn copy
                  QUERY_IN_STREAM
                  (-> SAMPLE_CSV io/file io/input-stream))))))

  (with-title "PG COPY in from rows BIN"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-in-rows conn
                        QUERY_IN_STREAM_BIN
                        (generate-rows)
                        {:copy-bin? true
                         :oids [oid/int4 oid/text oid/timestamp]}))))

  (with-title "PG COPY in from maps BIN"
    (pg/with-connection [conn pg-config]
      (quick-bench
       (pg/copy-in-maps conn
                        QUERY_IN_STREAM_BIN
                        (generate-maps)
                        [:id :name :created_at]
                        {:copy-bin? true
                         :oids [oid/int4 oid/text oid/timestamp]}))))

  (with-title "JDBC COPY in from rows"
    (with-open [conn (jdbc/get-connection
                      jdbc-config)]
      (quick-bench
       (let [copy
             (new CopyManager conn)

             ^ByteArrayOutputStream buf
             (new java.io.ByteArrayOutputStream)

             rows
             (generate-rows)]

         (with-open [writer (io/writer buf)]
           (csv/write-csv writer rows))

         (.copyIn copy
                  ^String QUERY_IN_STREAM
                  ^InputStream (-> buf (.toByteArray) io/input-stream))))))

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
                  ^OutputStream (OutputStream/nullOutputStream)))))))
