(ns pg.jdbc
  "
  The Next.JDBC-friendly wrapper. Mimics most of the
  `next.jdbc` functions and macros.

  https://github.com/seancorfield/next-jdbc/blob/develop/src/next/jdbc.clj
  "
  (:import
   clojure.lang.IPersistentMap
   clojure.lang.IFn
   org.pg.Connection
   org.pg.pool.Pool)
  (:require
   [clojure.set :as set]
   [pg.core :as pg]
   [pg.pool :as pool]))


(defprotocol IConnectable
  (-connect [this]))


(extend-protocol IConnectable

  Connection

  (-connect [this]
    this)

  Pool

  (-connect [this]
    (pool/borrow-connection this))

  IPersistentMap

  (-connect [this]
    (pg/connect this))

  Object

  (-connect [this]
    (pg/error! "Unsupported connection source: %s" this))

  nil

  (-connect [this]
    (pg/error! "Connection source cannot be null")))


(defn get-connection ^Connection [source]
  (-connect source))


(defn ->fn-execute ^IFn [expr]
  (cond

    (string? expr)
    pg/execute

    (pg/prepared-statement? expr)
    pg/execute-statement

    :else
    (pg/error! "Wrong execute expression: %s" expr)))


(defn execute!
  ([source sql-vec]
   (execute! source sql-vec nil))

  ([source sql-vec opt]

   (let [[expr & params]
         sql-vec

         fn-execute
         (->fn-execute expr)]

     (fn-execute (-connect source)
                 expr
                 (assoc opt :params params)))))


(defn prepare
  ([source sql-vec]
   (prepare source sql-vec nil))

  ([source sql-vec opt]
   (let [[sql & params] sql-vec]
     (pg/prepare (-connect source)
                 sql
                 (assoc opt :params params)))))


(defn execute-one!
  ([source sql-vec]
   (execute-one! source sql-vec nil))

  ([source sql-vec opt]
   (let [[expr & params]
         sql-vec

         fn-execute
         (->fn-execute expr)]

     (fn-execute (-connect source)
                 expr
                 (assoc opt
                        :params params
                        :first? true)))))


(defn execute-batch!
  [conn sql opts]
  (pg/error! "execute-batch! is not imiplemented"))


(defmacro on-connection [[bind source] & body]

  `(let [source# ~source]
     (cond

       (pool/pool? source#)
       (pool/with-connection [~bind source#]
         ~@body)

       (map? source#)
       (pg/with-connection [~bind source#]
         ~@body)

       :else
       (pg/error! "Unsupported connection source: %s" source#))))


(defn remap-tx-opts [jdbc-opt]
  (set/rename-keys jdbc-opt
                   {:isolation     :isolation-level
                    :read-only     :read-only?
                    :rollback-only :rollback?}))


;; ------------- untested


(defn transact
  ([source f]
   (transact source f nil))

  ([source f opt]
   (let [conn (-connect source)]
     (pg/with-tx [conn (remap-tx-opts opt)]
       (f conn)))))


(defmacro with-transaction
  [[bind source opts] & body]
  `(pg/with-tx [~bind
                (-connect ~source)
                ~@(when opts
                    `[(remap-tx-opts ~opts)])]
     ~@body))


(defn active-tx? [conn]
  (not (pg/idle? conn)))
