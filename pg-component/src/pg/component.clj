(ns pg.component
  (:import
   org.pg.Connection
   org.pg.pool.Pool)
  (:require
   [com.stuartsierra.component :as component]
   [pg.client :as pg]
   [pg.pool :as pool]))


(defn connection [config]
  (with-meta
   {`component/start
    (fn start [this]
      (pg/connect this))}))

#_
(extend Connection

  component/Lifecycle

  (start [this]
         ;; cannot be started?
         )

  (stop [this]
    (.close this)))
