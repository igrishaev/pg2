(ns pg.component
  (:import
   org.pg.Connection
   org.pg.Pool)
  (:require
   [com.stuartsierra.component :as component]
   [pg.core :as pg]
   [pg.pool :as pool]))


(defn connection [opt]
  (with-meta
    opt
    {`component/start
     (fn start [this]
       (pg/connect this))

     `component/stop
     (fn stop [this]
       this)}))


(extend-type Connection

  component/Lifecycle

  (start [this]
    this)

  (stop [this]
    (.close this)))


(defn pool [opt]
  (with-meta
    opt
    {`component/start
     (fn start [this]
       (pool/pool this))

     `component/stop
     (fn stop [this]
       this)}))


(extend-type Pool

  component/Lifecycle

  (start [this]
    this)

  (stop [this]
    (.close this)))
