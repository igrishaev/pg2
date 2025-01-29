(ns pg.source
  "
  A dedicated namespace for the ISource
  abstraction and its implementations.
  "
  (:require
   [pg.config :refer [->config]]
   [pg.common :refer [error!]])
  (:import
   clojure.lang.IPersistentMap
   org.pg.Connection
   org.pg.Pool
   org.pg.Config))


(defmacro unsupported! [src]
  `(error! "Unsupported data source: %s" ~src))


(defprotocol ISource
  "
  A set of actions that can be applied to any kind
  of a data source: a Clojure map, a Config object,
  a Connection, a Pool instance, or a URI string.
  "

  (-id ^UUID [this]
    "Get a unique ID of a data source.")

  (-closed? [this]
    "True if the source has been closed.")

  (-clone [this]
    "Create a new instance of this data source.")

  (-close [this]
    "Close a data source")

  (-to-config ^Config [this]
    "Turn this object into a Config instance.")

  (-borrow-connection [this]
    "Obtain a connection from a source. Don't call it directly.")

  (-return-connection [this conn]
    "Return a connection to a source. Don't call it directly."))


(extend-protocol ISource

  Connection

  (-id ^UUID [this]
    (.getId this))

  (-closed? [this]
    (.isClosed this))

  (-clone [this]
    (Connection/clone this))

  (-close [this]
    (.close this))

  (-to-config [this]
    (.getConfig this))

  (-borrow-connection [this]
    this)

  (-return-connection [this conn]
    nil)

  Pool

  (-id [this]
    (.getId this))

  (-closed? [this]
    (.isClosed this))

  (-clone [this]
    (Pool/clone this))

  (-close [this]
    (.close this))

  (-to-config [this]
    (.getConfig this))

  (-borrow-connection [this]
    (.borrowConnection this))

  (-return-connection [this conn]
    (.returnConnection this conn))

  String

  (-id [this]
    nil)

  (-closed? [this]
    false)

  (-clone [this]
    this)

  (-close [this]
    nil)

  (-to-config [this]
    (->config {:connection-uri this}))

  (-borrow-connection [this]
    (-> this -to-config Connection/connect))

  (-return-connection [this conn]
    (-close conn))

  IPersistentMap

  (-id [this]
    nil)

  (-closed? [this]
    false)

  (-clone [this]
    this)

  (-close [this]
    nil)

  (-to-config [this]
    (->config this))

  (-borrow-connection [this]
    (-> this -to-config Connection/connect))

  (-return-connection [this conn]
    (-close conn))

  Config

  (-id [this]
    nil)

  (-closed? [this]
    false)

  (-clone [this]
    this)

  (-close [this]
    nil)

  (-to-config [this]
    this)

  (-borrow-connection [this]
    (Connection/connect this))

  (-return-connection [this conn]
    (-close conn))

  Object

  (-id [this]
    (unsupported! this))

  (-closed? [this]
    (unsupported! this))

  (-clone [this]
    (unsupported! this))

  (-close [this]
    (unsupported! this))

  (-borrow-connection [this]
    (unsupported! this))

  (-to-config [this]
    (unsupported! this))

  (-return-connection [this conn]
    (unsupported! this))

  nil

  (-id [this]
    (unsupported! this))

  (-closed? [this]
    (unsupported! this))

  (-clone [this]
    (unsupported! this))

  (-close [this]
    (unsupported! this))

  (-to-config [this]
    (unsupported! this))

  (-borrow-connection [this]
    (unsupported! this))

  (-return-connection [this conn]
    (unsupported! this)))
