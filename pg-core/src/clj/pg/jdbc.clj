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
  Get a connection out from a pool or a Clojure map.
  "
  ^Connection [connectable]
  (cond

    (pool/pool? connectable)
    (pool/borrow-connection connectable)

    (map? connectable)
    (pg/connect connectable)

    (pg/connection? connectable)
    connectable

    :else
    (pg/error! "Unsupported connectable: %s" connectable)))


(defn get-connection ^Connection [connectable]
  (->connection connectable))


(defn prepare
  ([conn sql-vec]
   (prepare conn sql-vec nil))

  ([conn sql-vec _opt]
   (let [[sql & _params] sql-vec]
     (pg/prepare conn sql))))


(defn execute!
  ([conn sql-vec]
   (execute! conn sql-vec nil))

  ([conn sql-vec opt]
   (let [[sql & params] sql-vec]
     (pg/execute conn sql (assoc opt
                                 :params params)))))


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


;; TODO: pool support
(defmacro on-connection [[bind config] & body]
  `(pg/with-connection [~bind ~config]
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
  [[bind conn opts] & body]
  `(pg/with-tx [bind conn (remap-tx-opts opts)]
     ~@body))


(defn active-tx? [conn]
  (not (pg/idle? conn)))
