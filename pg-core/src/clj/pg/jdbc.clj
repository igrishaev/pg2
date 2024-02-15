(ns pg.jdbc
  "
  The Next.JDBC-friendly wrapper. Mimics most of the
  `next.jdbc` functions and macros.

  https://github.com/seancorfield/next-jdbc/blob/develop/src/next/jdbc.clj
  "
  (:require
   [pg.core :as pg]))


(defn get-connection [config]
  (pg/connect config))


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


;; TODO: not implemented
(defn execute-batch!
  ([conn sql opts]))


(defmacro on-connection [[bind config] & body]
  `(pg/with-connection [~bind ~config]
     ~@body))


(defn transact
  ([conn f]
   (transact conn f nil))

  ([conn f opt]
   (pg/with-tx [conn opt]
     (f conn))))


;; TODO: remap options
(defmacro with-transaction
  "

  The options map supports:
  * `:isolation` -- `:none`, `:read-committed`, `:read-uncommitted`,
      `:repeatable-read`, `:serializable`,
  * `:read-only` -- `true` / `false` (`true` will make the `Connection` readonly),
  * `:rollback-only` -- `true` / `false` (`true` will make the transaction
      rollback, even if it would otherwise succeed)."
  [[sym transactable opts] & body]
  `(pg/with-tx [sym transactable opts]
     ~@body)


  #_
  (let [con (vary-meta sym assoc :tag 'java.sql.Connection)]
    `(transact ~transactable (^{:once true} fn* [~con] ~@body) ~(or opts {}))))


(defn active-tx? [conn]
  (not (pg/idle? conn)))
