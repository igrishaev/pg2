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
   [pg.client :as pg])
  (:import
   java.io.Writer
   java.util.Map
   org.pg.Connection
   org.pg.pool.Pool
   org.pg.pool.PoolConfig
   org.pg.pool.PoolConfig$Builder))


(defn ->pool-config ^PoolConfig [opt]

  (let [{:keys [min-size
                max-size
                ms-lifetime]}
        opt]

    (cond-> (PoolConfig/builder)

      min-size
      (.minSize min-size)

      max-size
      (.maxSize max-size)

      ms-lifetime
      (.msLifetime ms-lifetime)

      :finally
      (.build))))


(defn pool

  (^Pool [^Map conn-config]
   (new Pool (pg/->conn-config conn-config)))

  (^Pool [^Map conn-config ^Map pool-config]
   (new Pool
        (pg/->conn-config conn-config)
        (->pool-config pool-config))))


(defn close [^Pool pool]
  (.close pool))


(defn used-count ^Integer [^Pool pool]
  (.usedCount pool))


(defn free-count ^Integer [^Pool pool]
  (.freeCount pool))


(defmacro with-pool
  [[bind conn-config pool-config] & body]
  `(with-open [~bind (pool ~conn-config ~pool-config)]
     ~@body))


(defmacro with-connection [[bind pool] & body]
  `(let [pool#
         ~pool

         ~(with-meta bind {:tag `Connection})
         (.borrowConnection pool#)]
     (try
       ~@body
       (finally
         (.returnConnection pool# ~bind)))))


(defn stats [^Pool pool]
  {:free (free-count pool)
   :used (used-count pool)})


(defn closed? ^Boolean [^Pool pool]
  (.isClosed pool))


(defmethod print-method Pool
  [^Pool pool ^Writer writer]
  (.write writer (.toString pool)))
