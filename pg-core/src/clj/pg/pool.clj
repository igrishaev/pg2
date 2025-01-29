(ns pg.pool
  "
  A simple connection pool. Runs open connections in two
  data structures: a queue of free connections and a map
  of busy connections. The connections are taken from a tail
  of the queue and put back into the head.

  Every time the connection is borrowed, it's check for expiration.
  An expired connection gets closed, and the one is produced.

  Should all free connections are in use at the moment and the client
  is going to borrow another one, an exception is triggered.

  When a connection is put back, it's checked for expiration and for
  the transaction status. Connections that are in the error state
  are closed; Connections that are in a transaction are rolled back.
  "
  (:require
   [pg.config :refer [->config]]
   [pg.core :as pg])
  (:import
   java.io.Writer
   java.util.Map
   java.util.UUID
   org.pg.Connection
   org.pg.Pool))


(defn used-count
  "
  Return the current number of busy connections.
  "
  ^Integer [^Pool pool]
  (.usedCount pool))


(defn free-count
  "
  Return the current number of free connections.
  "
  ^Integer [^Pool pool]
  (.freeCount pool))


(defn stats
  "
  Return both free and used connection amount as a map.
  "
  [^Pool pool]
  {:free (free-count pool)
   :used (used-count pool)})


(defn replenish-connections
  "
  Forcibly run a task that determines how many new
  free connections should be created, and creates them.
  The number is calculated as follows:

  gap = min-size - size(free-conns) - size(used-conns)

  When gap is > 0, the corresponding number of free connections
  is created.

  Blocks the pool.
  "
  [^Pool pool]
  (.replenishConnections pool))


;;
;; THE ABYSS OF DEPRECATED
;;

(defn ^:deprecated id
  "
  Deprecated! Use `pg.core/id`.
  "
  [^Pool pool]
  (pg/id pool))


(defn ^:deprecated pool?
  "
  Deprecated! Use `pg.core/pool?`.
  "
  [x]
  (pg/pool? x))


(defn ^:deprecated pool
  "
  Deprecated! Use `pg.core/pool`.
  "
  ^Pool [& args]
  (apply pg/pool args))


(defn ^:deprecated close
  "
  Deprecated! Use `pg.core/close`.
  "
  [^Pool pool]
  (pg/close pool))

(defmacro ^:deprecated with-pool
  "
  Deprecated! Use `pg.core/with-pool`.
  "
  [& args]
  `(pg/with-pool ~@args))


(defmacro ^:deprecated with-connection
  "
  Deprecated! Use `pg.core/with-connection`.
  "
  [& args]
  `(pg/with-connection ~@args))


(defmacro ^:deprecated with-conn
  "
  Deprecated! Use `pg.core/with-conn`.
  "
  [& args]
  `(pg/with-conn ~@args))


(defn ^:deprecated closed?
  "
  Deprecated! Use `pg.core/closed?`
  "
  [^Pool pool]
  (pg/closed? pool))
