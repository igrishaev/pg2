(ns pg.bench
  (:import
   org.postgresql.copy.CopyManager
   org.pg.Connection
   org.pg.ConnConfig$Builder
   org.postgresql.util.PGobject
   java.sql.PreparedStatement
   java.time.LocalDateTime)
  (:use criterium.core)
  (:require
   [pg.client :as pg]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [next.jdbc.prepare :as prepare]
   [jsonista.core :as json]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))


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


(defn generate-csv []
  (let [rows
        (for [x (range 0 1000000)]
          [x
           (str "name" x)
           (LocalDateTime/now)])]
    (with-open [writer (-> "foo.csv"
                           io/file
                           io/writer)]
      (csv/write-csv writer rows))))


(defn title [line]
  (println "-----------------------")
  (println line)
  (println "-----------------------"))


(defn -main [& args]

  #_
  (generate-csv)

  #_
  (title "pg JSON select")
  #_
  (pg/with-connection [conn pg-config]
    (with-progress-reporting
      (quick-bench
       (pg/execute conn
                   QUERY_SELECT_RANDOM_VAL
                   #_
                   QUERY_SELECT_JSON
                   ))))

  #_
  (title "next.JDBC JSON select")
  #_
  (with-open [conn (jdbc/get-connection
                    jdbc-config)]

    (with-progress-reporting
      (quick-bench
       (jdbc/execute! conn
                      [QUERY_SELECT_RANDOM_VAL]
                      #_
                      [QUERY_SELECT_JSON]
                      {:as rs/as-unqualified-maps}))))

  #_
  (title "pg insert values")
  #_
  (pg/with-connection [conn pg-config]
    (pg/execute conn QUERY_TABLE)
    (pg/with-statement [stmt
                        conn
                        QUERY_INSERT_PG]
      (with-progress-reporting
        (quick-bench
         (let [x (rand-int 10000000)]
           (pg/execute-statement conn
                                 stmt
                                 {:params [x,
                                           (format "name%s" x)
                                           (LocalDateTime/now)]}))))))

  #_
  (title "next.JDBC insert values")
  #_
  (with-open [conn (jdbc/get-connection
                    jdbc-config)]

    (jdbc/execute! conn [QUERY_TABLE])

    (with-progress-reporting
      (quick-bench
       (let [x (rand-int 10000000)]
         (jdbc/execute! conn
                        [QUERY_INSERT_JDBC
                         x,
                         (format "name%s" x)
                         (LocalDateTime/now)
                         ])))))


  (title "PG COPY in from a stream")

  (pg/with-connection [conn pg-config]
    (with-progress-reporting
      (quick-bench
       (pg/copy-in conn
                   "copy aaa (id, name, created_at) from STDIN WITH (FORMAT CSV)"
                   (-> "foo.csv" io/file io/input-stream)))))

  (title "JDBC COPY in from a stream")

  (with-open [conn (jdbc/get-connection
                    jdbc-config)]

    (jdbc/execute! conn [QUERY_TABLE])

    (with-progress-reporting
      (quick-bench
       (let [copy
             (new CopyManager conn)]

         (.copyIn copy
                  "copy aaa (id, name, created_at) from STDIN WITH (FORMAT CSV)"
                  (-> "foo.csv" io/file io/input-stream)))

)))



)
