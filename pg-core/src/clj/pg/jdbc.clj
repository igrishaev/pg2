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


(def opt-defaults
  {:kebab-keys? true})


;;
;; Fetch connection from various sources (config, pool).
;;

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


(defn get-connection
  "
  Return a connection object from a source which might be
  a config map, a pool, or a connection. Not recommended
  to use as there is a chance to leave the connection not
  closed. Consider `on-connection` instead.
  "
  ^Connection [source]
  (-connect source))


(defmacro on-connection
  "
  Perform a block of code while the `bind` symbol is bound
  to a `Connection` object. If the source is a config map,
  the connection gets closed. When the source is a pool,
  the connection gets borrowed and returned afterwards.
  For existing connection, nothing is happening at the end.
  "
  [[bind source] & body]

  `(let [source# ~source]
     (cond

       (pg/connection? source#)
       (let [~bind source#]
         (do ~@body))

       (pool/pool? source#)
       (pool/with-connection [~bind source#]
         ~@body)

       (map? source#)
       (with-open [~bind (get-connection source#)]
         ~@body)

       :else
       (pg/error! "Unsupported connection source: %s" source#))))


(defn ->fn-execute ^IFn [expr]
  (cond

    (string? expr)
    pg/execute

    (pg/prepared-statement? expr)
    pg/execute-statement

    :else
    (pg/error! "Wrong execute expression: %s" expr)))


(defn execute!
  "
  Given a source and an SQL vector like [expr & params],
  execute a query and return a result.

  The `expr` might be both a string representing a SQL query
  or an instance of the `PreparedStatement` class.
  "
  ([source sql-vec]
   (execute! source sql-vec nil))

  ([source sql-vec opt]

   (let [[expr & params]
         sql-vec

         fn-execute
         (->fn-execute expr)]

     (on-connection [conn source]
       (fn-execute conn
                   expr
                   (-> opt-defaults
                       (merge opt)
                       (assoc :params params)))))))


(defn execute-one!
  "
  Like `execute!` but returns only the first row.
  "
  ([source sql-vec]
   (execute-one! source sql-vec nil))

  ([source sql-vec opt]
   (let [[expr & params]
         sql-vec

         fn-execute
         (->fn-execute expr)]

     (on-connection [conn source]
       (fn-execute conn
                   expr
                   (-> opt-defaults
                       (merge opt)
                       (assoc :params params
                              :first? true)))))))


(defn prepare
  "
  Prepare and return an instance of the `PreparedStatement` class.

  The `sql-vec` must be a vector like `[sql & params]`. The `params`,
  although not requred, might be used for type hints, for example:

  ```
  ['select $1 as foo', 42]
  ```

  Without passing 42, the parser won't know the desired type of $1
  and it will be a default one which is text.

  Once prepared, the statement might be used continuously
  with different parameters.

  Don't share it with other connections.
  "
  ([source sql-vec]
   (prepare source sql-vec nil))

  ([source sql-vec opt]
   (let [[sql & params] sql-vec]
     (on-connection [conn source]
       (pg/prepare conn
                   sql
                   (assoc opt :params params))))))


(defn execute-batch!
  "
  Performs batch execution on the server side (TBD).
  "
  [conn sql opts]
  (pg/error! "execute-batch! is not imiplemented"))


(defn remap-tx-opts [jdbc-opt]
  (set/rename-keys jdbc-opt
                   {:isolation     :isolation-level
                    :read-only     :read-only?
                    :rollback-only :rollback?}))


(defmacro with-transaction
  "
  Execute a block of code in a transaction. The connection
  with the transaction opened is bound to the `bind` symbol.
  The source might be a config map, a connection, or a pool.
  The `opts` is a map that accepts next.jdbc parameters for
  transaction, namely:

  - `:isolation`: one of these keywords:
    - `:read-committed`
    - `:read-uncommitted`
    - `:repeatable-read`
    - `:serializable`
  - `:read-only`: true / false;
  - `:rollback-only`; true / false.

  Return the result of the body block.
  "
  [[bind source opts] & body]
  `(on-connection [~bind ~source]
     (pg/with-tx [~bind
                  ~@(when opts
                      `[(remap-tx-opts ~opts)])]
       ~@body)))


(defn transact
  "
  Having a source and a function that accepts a connection,
  do the following steps:

  - get a connection from the source;
  - start a new transaction in this connection;
  - run the function with the connection;
  - either commit or rollback the transaction depending
    on whether there was an exception;
  - if there was not, return the result of the function.

  The `opt` params are similar to `with-transaction` options.
  "
  ([source f]
   (transact source f nil))

  ([source f opt]
   (with-transaction [conn source opt]
     (f conn))))


(defn active-tx?
  "
  True if the connection is a transaction at the moment.
  Even if the transaction is in an error state, the result
  will be true.
  "
  [conn]
  (not (pg/idle? conn)))
