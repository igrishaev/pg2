(ns pg.jdbc
  "
  The Next.JDBC-friendly wrapper. Mimics most of the
  `next.jdbc` functions and macros.

  https://github.com/seancorfield/next-jdbc/blob/develop/src/next/jdbc.clj
  "
  (:import
   org.pg.Connection)
  (:require
   [clojure.set :as set]
   [pg.core :as pg]
   [pg.pool :as pool]))


(defn ->connection
  "
  Get a connection out from a source (pool, Clojure map).
  "
  ^Connection [source]
  (cond

    (pool/pool? source)
    (pool/borrow-connection source)

    (map? source)
    (pg/connect source)

    (pg/connection? source)
    source

    :else
    (pg/error! "Unsupported connection source: %s" source)))


(defn get-connection ^Connection [source]
  (->connection source))


(defn execute!
  ([source sql-vec]
   (execute! source sql-vec nil))

  ([source sql-vec opt]

   (let [[expr & params]
         sql-vec

         fn-execute
         (cond

           (string? expr)
           pg/execute

           (pg/prepared-statement? expr)
           pg/execute-statement

           :else
           (pg/error! "Wrong execute expression: %s" expr))]

     (fn-execute (->connection source)
                 expr
                 (assoc opt :params params)))))


;; TODO: pass OIDS
(defn prepare
  ([source sql-vec]
   (prepare source sql-vec nil))

  ([source sql-vec opt]
   (let [[sql & _params] sql-vec]
     (pg/prepare (->connection source) sql))))





(defn execute-one!
  ([conn sql-vec]
   (execute-one! conn sql-vec nil))

  ([conn sql-vec opt]
   (let [[sql & params] sql-vec]
     (pg/execute conn sql (assoc opt
                                 :params params
                                 :first? true)))))


(defn execute-batch!
  [conn sql opts]
  (pg/error! "execute-batch! is not imiplemented"))


(defmacro on-connection [[bind connectable] & body]
  `(pg/with-connection [~bind (->connection ~connectable)]
     ~@body))


(defn transact
  ([conn f]
   (transact conn f nil))

  ([conn f opt]
   (pg/with-tx [conn opt]
     (f conn))))


(defn remap-tx-opts [jdbc-opt]
  (set/rename-keys jdbc-opt
                   {:isolation     :isolation-level
                    :read-only     :read-only?
                    :rollback-only :rollback?}))


(defmacro with-transaction
  [[bind connectable opts] & body]
  `(pg/with-tx [~bind
                (->connection ~connectable)
                ~@(when opts
                    `[(remap-tx-opts ~opts)])]
     ~@body))


(defn active-tx? [conn]
  (not (pg/idle? conn)))
