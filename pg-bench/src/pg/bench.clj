(ns pg.bench
  (:import
   org.pg.Connection
   org.pg.ConnConfig$Builder
   org.postgresql.util.PGobject
   java.sql.PreparedStatement)
  (:use criterium.core)
  (:require
   [pg.client :as pg]
   [next.jdbc.prepare :as prepare]
   [jsonista.core :as json]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))


(def USER "ivan")
;;(def USER "test")

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
   :binary-decode? true})


(def jdbc-config
  {:dbtype "postgres"
   :port PORT
   :dbname USER
   :user USER
   :password USER})


(def QUERY_SELECT_JSON
  "select '[1, 2, 3]'::jsonb from generate_series(1,50000)")

(def QUERY_SELECT_RANDOM_VAL
  "select random() as x from generate_series(1,50000)")


#_["select * from generate_series(1,50000)"]

(defn -main [& args]

  (println "pg JSON select")

  (pg/with-connection [conn pg-config]
    (with-progress-reporting
      (quick-bench
       (pg/execute conn
                   QUERY_SELECT_RANDOM_VAL
                   #_QUERY_SELECT_JSON
                   ))))

  (println "next.JDBC JSON select")

  (with-open [conn (jdbc/get-connection
                    jdbc-config)]

    (with-progress-reporting
      (quick-bench
       (jdbc/execute! conn
                      [QUERY_SELECT_RANDOM_VAL]
                      #_[QUERY_SELECT_JSON]
                      {:as rs/as-unqualified-maps})))))
