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
   [pg.core :as pg])
  (:import
   java.io.Writer
   java.util.Map
   java.util.UUID
   org.pg.Connection
   org.pg.Pool))


(defn id
  "
  Get a unique ID of the pool.
  "
  ^UUID [^Pool pool]
  (.getId pool))


(defn pool?
  "
  True if a value is a Pool instance.
  "
  [x]
  (instance? Pool x))


(defn pool
  "
  Run a new Pool from a config map.
  "
  (^Pool [^Map opt]
   (Pool/create (pg/->config opt)))

  (^Pool [^String host ^Integer port ^String user ^String password ^String database]
   (Pool/create host port user password database)))


(defn close
  "
  Close the pool by terminating all the connections,
  both free and used.
  "
  [^Pool pool]
  (.close pool))


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


(defmacro with-pool
  "
  Execute the body while the `bind` symbol is bound
  to a new Pool instance. Close the pool afterwards.
  "
  [[bind config] & body]
  `(with-open [~bind (pool ~config)]
     ~@body))


(defn borrow-connection
  "
  Borrow a connection from a pool.
  "
  ^Connection [^Pool pool]
  (.borrowConnection pool))


(defmacro with-connection
  "
  Execute the body while the `bind` symbol is bound
  to a borrowed Connection instance. The connection
  is marked is busy and won't be available for other
  consumers. Return the connection to the pool when
  exiting the macro.

  When no connections available, throw an exception.
  "
  [[bind pool] & body]
  (let [POOL
        (with-meta (gensym "POOL")
          {:tag `Pool})]
    `(let [~POOL
           ~pool

           ~(with-meta bind {:tag `Connection})
           (.borrowConnection ~POOL)]
       (try
         ~@body
         (finally
           (.returnConnection ~POOL ~bind))))))


(defn stats
  "
  Return both free and used connection amount as a map.
  "
  [^Pool pool]
  {:free (free-count pool)
   :used (used-count pool)})


(defn closed?
  "
  True if the pool has been closed before.
  "
  ^Boolean [^Pool pool]
  (.isClosed pool))


(defmethod print-method Pool
  [^Pool pool ^Writer writer]
  (.write writer (.toString pool)))


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
