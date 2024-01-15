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
   org.pg.Connection
   org.pg.pool.Pool
   org.pg.pool.PoolConfig
   org.pg.pool.PoolConfig$Builder))


(defn pool?
  "
  True if a value is a Pool instance.
  "
  [x]
  (instance? Pool x))


(defn ->pool-config
  "
  Build a PoolConfig instance from a Clojure map.
  "
  ^PoolConfig [opt]
  (let [{:keys [pool-min-size
                pool-max-size
                pool-ms-lifetime
                pool-log-level]}
        opt]

    (cond-> (PoolConfig/builder)

      pool-min-size
      (.minSize pool-min-size)

      pool-max-size
      (.maxSize pool-max-size)

      pool-ms-lifetime
      (.msLifetime pool-ms-lifetime)

      pool-log-level
      (.logLevel (pg/->LogLevel pool-log-level))

      :finally
      (.build))))


(defn pool
  "
  Run a new Pool from a config map.
  "
  ^Pool [^Map opt]
  (new Pool
       (pg/->conn-config opt)
       (->pool-config opt)))


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
