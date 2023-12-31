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


;; (def USER "wzhivga")
(def USER "ivan")


;; :decode-key-fn here specifies that JSON-keys will become keywords:
(def mapper
  (json/object-mapper {:decode-key-fn keyword}))

(def ->json json/write-value-as-string)

(def <-json #(json/read-value % mapper))


(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
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

;; if a row contains a PGobject then we'll convert them to Clojure data
;; while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v)))


(def cfg
  (-> (new ConnConfig$Builder USER USER)
      (.host "127.0.0.1")
      (.port 15432)
      (.password USER)
      (.binaryEncode false)
      (.binaryDecode false)
      (.build)))




(comment

  ;; jdbc
  (with-open [conn (jdbc/get-connection
                    {:dbtype "postgres"
                     :port 15432
                     :dbname USER
                     :user USER
                     :password USER})]

    (with-progress-reporting
      (quick-bench
          (do
            (jdbc/execute! conn
                           ["select '[1, 2, 3]'::jsonb from generate_series(1,50000)"]
                           #_["select * from generate_series(1,50000)"]
                           {:as rs/as-unqualified-maps})
            nil)
        :verbose)))

  ;; pg
  (pg/with-connection [conn {:host "127.0.0.1"
                             :port 15432
                             :user USER
                             :password USER
                             :database USER
                             :binary-encode? true
                             :binary-decode? true}]

    (with-progress-reporting
      (quick-bench
          (do
            (pg/execute conn "select '[1, 2, 3]'::jsonb from generate_series(1,50000)")
            #_
            (pg/execute conn "select * from generate_series(1,50000)")
            #_
            (pg/query conn "select * from generate_series(1,50000)")
            nil)
        :verbose)))

  )







(comment

  (time
   (do
     (let [conn (jdbc/get-connection
                 {:dbtype "postgres"
                  :port 15432
                  :dbname USER
                  :user USER
                  :password USER})]

       (jdbc/execute! conn
                      ["select '[1, 2, 3]'::jsonb from generate_series(1,5000000)"]
                      #_
                      {:as rs/as-unqualified-maps}))
     nil))

  ;; 8000.900308
  (let [conn (jdbc/get-connection
              {:dbtype "postgres"
               :port 15432
               :dbname USER
               :user USER
               :password USER})]

    (with-progress-reporting
      (quick-bench

          (jdbc/execute! conn
                         ["select * from generate_series(1,50000)"]
                         {:as rs/as-unqualified-maps})

          :verbose)))


  #_
  (def ^Connection -c
    (new Connection
         "127.0.0.1"
         (int 15432)
         USER
         USER
         USER))

  (def ^Connection -c
    (new Connection cfg))


  (time
   (do
     (.execute -c "select * from generate_series(1,5000000)")
     nil))

  (time
   (do

     #_(.query -c "select '[1, 2, 3]'::jsonb from generate_series(1,5000000)")

     (.query -c "select * from generate_series(1,50000)")
     nil))

  (with-progress-reporting
    (quick-bench
        (.execute -c "select * from generate_series(1,50000)")
        :verbose))


  #_
  (time
   (do
     (.query -c "select * from generate_series(1,5000000)")
     nil))


  )
