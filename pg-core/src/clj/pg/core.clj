(ns pg.core
  "
  Common API to communicate with PostgreSQL server.
  "
  (:require
   [clojure.string :as str]
   [pg.common :refer [error!]]
   [pg.execute-params :refer [->execute-params]]
   [pg.source :as src]
   [pg.ssl #_(load a reader tag)] )
  (:import
   clojure.lang.IPersistentMap
   clojure.lang.Keyword
   com.fasterxml.jackson.databind.ObjectMapper
   java.io.InputStream
   java.io.OutputStream
   java.io.Reader
   java.io.Writer
   java.lang.AutoCloseable
   java.net.URI
   java.nio.ByteBuffer
   java.nio.charset.Charset
   java.time.ZoneId
   java.util.List
   java.util.Map
   java.util.UUID
   org.pg.CancelTimer
   org.pg.Config
   org.pg.Config$Builder
   org.pg.Connection
   org.pg.ExecuteParams
   org.pg.ExecuteParams$Builder
   org.pg.Pool
   org.pg.PreparedStatement
   org.pg.clojure.RowMap
   org.pg.codec.CodecParams
   org.pg.codec.CodecParams$Builder
   org.pg.enums.CopyFormat
   org.pg.enums.OID
   org.pg.enums.SSLValidation
   org.pg.enums.TXStatus
   org.pg.enums.TxLevel
   org.pg.error.PGError
   org.pg.error.PGErrorResponse
   org.pg.json.JSON
   org.pg.json.JSON$Wrapper
   org.pg.processor.IProcessor
   org.pg.processor.Processors))


(def ^CopyFormat COPY_FORMAT_BIN CopyFormat/BIN)
(def ^CopyFormat COPY_FORMAT_CSV CopyFormat/CSV)
(def ^CopyFormat COPY_FORMAT_TAB CopyFormat/TAB)


;;
;; Connection
;;

(defn connect
  "
  Connect to the database. Given a Clojure config map
  or a URI string, establish a TCP connection with the
  server and run the authentication pipeline. Returns
  an instance of the Connection class.
  "
  (^Connection [src]
   (src/-borrow-connection src))

  (^Connection [^String host ^Integer port ^String user ^String password ^String database]
   (Connection/connect host port user password database)))


(defmacro with-connection
  "
  Perform a block of code while the `bind` symbol is bound
  to a `Connection` object. If the `src` is a config map,
  the connection gets closed afterwards. When the `src` is
  a pool, the connection gets borrowed and returned to the
  pool without closing. For a connection, nothing happens
  when exiting the macro.
  "
  [[bind src] & body]
  `(let [src#
         ~src
         ~(with-meta bind {:tag `Connection})
         (src/-borrow-connection src#)]
     (try
       ~@body
       (finally
         (src/-return-connection src# ~bind)))))


(defmacro with-conn
  "
  Just a shorter version of `with-connection`.
  "
  [[bind src] & body]
  `(with-connection [~bind ~src]
     ~@body))


(let [-mapping
      {TXStatus/IDLE :I
       TXStatus/TRANSACTION :T
       TXStatus/ERROR :E}]

  (defn status
    "
    Return the current transaction status, one of :I, :T, or :E.
    "
    ^Keyword [^Connection conn]
    (get -mapping (.getTxStatus conn))))


(defmacro with-lock
  "
  Perform a block of code while the connection is locked
  (no other threads can interfere).
  "
  [[conn] & body]
  `(with-open [_# (.getLock ~conn)]
     ~@body))


(defn idle?
  "
  True if the connection is in the IDLE state.
  "
  ^Boolean [^Connection conn]
  (.isIdle conn))


(defn in-transaction?
  "
  True if the connection is in TRANSACTION at the moment.
  "
  ^Boolean [^Connection conn]
  (.isTransaction conn))


(defn tx-error?
  "
  True if the transaction has failed but hasn't been
  rolled back yet.
  "
  ^Boolean [^Connection conn]
  (.isTxError conn))


(defn get-parameter
  "
  Return a certain connection parameter by its name, e.g.
  'server_encoding', 'application_name', etc.
  "
  ^String [^Connection conn ^String param]
  (.getParam conn param))


(defn get-parameters
  "
  Return a {String->String} map of all the connection parameters.
  "
  ^Map [^Connection conn]
  (.getParams conn))


(defn ^UUID id
  "
  Get a unique ID of this source (a Connection or a Pool).
  "
  [src]
  (src/-id src))


(defn close
  "
  Close a data source. Closing a Connection terminates
  a session on the server side. Closing a Pool object
  terminates all open connections.
  "
  [src]
  (src/-close src))


(defn closed?
  "
  Whether the data source was closed. A closed source
  cannot be reused again.
  "
  [src]
  (src/-closed? src))


(defn clone
  "
  Produce another instance of a source with the same
  configuration.
  "
  [src]
  (src/-clone src))


(defn pid
  "
  Get PID of the connection on the server.
  "
  ^Integer [^Connection conn]
  (.getPid conn))


(defn created-at
  "
  Get the connection creation time as a Unix timestamp (ms).
  "
  ^Long [^Connection conn]
  (.getCreatedAt conn))


(defn close-statement
  "
  Close a prepared statement.
  "
  [^Connection conn ^PreparedStatement statement]
  (.closeStatement conn statement))


;;
;; Prepared statement
;;

(defn prepared-statement?
  "
  True if it's an instance of the PreparedStatement class.
  "
  [x]
  (instance? PreparedStatement x))


(defmethod print-method PreparedStatement
  [^PreparedStatement conn ^Writer writer]
  (.write writer (.toString conn)))


(defn prepare
  "
  Get a new prepared statement from a raw SQL string.
  The SQL might have parameters. There is an third
  optional parameter `oids` to specify the non-default types
  of the parameters. Must be a List of OID enum.

  The function returns an instance of the `PreparedStatement`
  class bound to the current connection.

  Must be called against a Connection object since prepared
  statements cannot be shared across multiple connections.
  "
  (^PreparedStatement
   [^Connection conn ^String sql]
   (.prepare conn sql ExecuteParams/INSTANCE))

  (^PreparedStatement
   [^Connection conn ^String sql ^Map opt]
   (.prepare conn sql (->execute-params opt))))


(defn close-cached-statements
  "
  Close all the cached prepared statements
  and clean up the cache.
  "
  ^Integer [^Connection conn]
  (.closeCachedPreparedStatements conn))


(defn execute-statement
  "
  Execute a given prepared statement and return a result.
  The way the result is processed heavily depends on options.
  "
  ([^Connection conn ^PreparedStatement statement]
   (.executeStatement conn statement ExecuteParams/INSTANCE))

  ([^Connection conn ^PreparedStatement statement ^Map opt]
   (.executeStatement conn statement (->execute-params opt))))


(defn execute
  "
  Execute a SQL expression and return a result. Arguments:
  - `src` is a data source (a Connection, a Pool, a map, a URI string);
  - `sql` is a SQL string expression;
  - `opt` is a map of options.

  This function is a series of steps:
  - prepare a statement;
  - bind parameters and obtain a portal;
  - describe the portal;
  - get the data from the portal;
  - close the portal;
  - close the statement;
  - process the result of the fly.
  "
  ([src ^String sql]
   (with-conn [conn src]
     (.execute conn sql ExecuteParams/INSTANCE)))

  ([src ^String sql ^Map opt]
   (with-conn [conn src]
     (.execute conn sql (->execute-params opt)))))


(defmacro with-timeout
  "
  Set a timeout in which, if a query has not been executed in time,
  a cancellation request is sent to the server. Wrap a query with
  that macro when there is a chance for a server to freeze completely
  due to a non-optimized SQL. The `ms-timeout` is amount of milliseconds
  in which the cancel request will be sent.
  "
  [[^Connection conn ms-timeout] & body]
  (let [init
        (if ms-timeout
          `(new CancelTimer ~conn ~ms-timeout)
          `(new CancelTimer ~conn))]
    `(with-open [timer# ~init]
       ~@body)))


(defmacro with-statement
  "
  Execute the body while the `bind` symbol is bound to the
  new PreparedStatement instance. The statement gets created
  from the SQL expression and optional list of OIDs. It gets
  closed when exiting the macro.
  "
  [[bind conn sql oids] & body]
  (let [CONN (gensym "conn")]
    `(let [~CONN
           ~conn

           ~bind
           ~(if oids
              `(prepare ~CONN ~sql ~oids)
              `(prepare ~CONN ~sql))]
       (try
         ~@body
         (finally
           (close-statement ~CONN ~bind))))))


(defn query
  "
  Run a SQL expression WITH NO parameters. The result
  is always sent back in text mode (binary mode doesn't
  work with the QUERY API). Arguments:
  - `src` is a data source (a Connection, a Pool, a map, a URI string);
  - `sql` is a SQL string without parameters (e.g. $1, etc);
  - `opt` is a map of options.
  "
  ([src ^String sql]
   (with-conn [conn src]
     (.query conn sql ExecuteParams/INSTANCE)))

  ([src ^String sql ^Map opt]
   (with-conn [conn src]
     (.query conn sql (->execute-params opt)))))


(defn cancel-request
  "
  Send a cancellation request to the server. MUST be called
  in another thread! The cancellation is meant to interrupt
  a query that has frozen the server. There is no 100% guarantee
  it will work.

  Not recommended to use directly. See the `with-timeout` macro.
  "
  [^Connection conn]
  (Connection/cancelRequest conn))


(defn copy-out
  "
  Transfer the data from the server to the client using
  COPY protocol. The SQL expression must be something like this:

  `COPY ... TO STDOUT ...`

  The `out` parameter must be an instance of OutputStream.

  The function doesn't close the stream assuming you can reuse it
  for multiple COPY OUT sessions.

  The `opt` map allows to specify the data format, CSV delimiters
  other options (see the docs in README).

  Return the number of rows read from the server.
  "
  ([src ^String sql ^OutputStream out]
   (with-conn [conn src]
     (copy-out conn sql out nil)))

  ([src ^String sql ^OutputStream out ^Map opt]
   (with-conn [conn src]
     (.copy conn
            sql
            (-> opt
                (assoc :output-stream out)
                (->execute-params))))))


(defn copy-in
  "
  Transfer the data from the client to the server using
  COPY protocol. The SQL expression must be something liek this:

  `COPY ... FROM STDIN ...`

  The `in` parameter is an instance of `InputStream`. The function
  doesn't close the stream assuming you can reuse it.

  The `opt` map is used to specify format, CSV delimiters, type hints
  and other options (see README).

  Return the number of rows processed by the server.
  "
  ([src ^String sql ^InputStream in]
   (copy-in src sql in nil))

  ([src ^String sql ^InputStream in ^Map opt]
   (with-conn [conn src]
     (.copy conn
            sql
            (-> opt
                (assoc :input-stream in)
                (->execute-params))))))


(defn copy-in-rows
  "
  Like `copy-in` but accepts not an input stream but a list
  of rows. Each row must be a list of values. The list might be
  lazy. Return a number of rows processed.
  "
  ([src ^String sql ^List rows]
   (copy-in-rows src sql rows nil))

  ([src ^String sql ^List rows ^Map opt]
   (with-conn [conn src]
     (.copy conn
            sql
            (-> opt
                (assoc :copy-in-rows rows)
                (->execute-params))))))


(defn copy-in-maps
  "
  Like `copy-in` but accepts a list of Clojure maps.

  The `keys` argument is list of keys which is used to convert
  each map into a tuple.

  The `opt` argument is a map of options (COPY format, delimiters, etc).

  Return the number of rows processed by the server.
  "
  ([src ^String sql ^List maps ^List keys]
   (copy-in-maps src sql maps keys nil))

  ([src ^String sql ^List maps ^List keys ^Map opt]
   (with-conn [conn src]
     (.copy conn
            sql
            (-> opt
                (assoc :copy-in-maps maps
                       :copy-in-keys keys)
                (->execute-params))))))


;;
;; Transactions
;;

(def ^TxLevel TX_SERIALIZABLE     TxLevel/SERIALIZABLE)
(def ^TxLevel TX_REPEATABLE_READ  TxLevel/REPEATABLE_READ)
(def ^TxLevel TX_READ_COMMITTED   TxLevel/READ_COMMITTED)
(def ^TxLevel TX_READ_UNCOMMITTED TxLevel/READ_UNCOMMITTED)


(defn ->tx-level
  "
  Turn a keyword or a string into on instance of TxLevel.
  "
  ^TxLevel [level]

  (case level

    (:serializable "SERIALIZABLE")
    TxLevel/SERIALIZABLE

    (:repeatable-read "REPEATABLE READ")
    TxLevel/REPEATABLE_READ

    (:read-committed "READ COMMITTED")
    TxLevel/READ_COMMITTED

    (:read-uncommitted "READ UNCOMMITTED")
    TxLevel/READ_UNCOMMITTED

    (nil :none "NONE")
    TxLevel/NONE

    ;; else
    (error! "wrong transaction isolation level: %s" level)))


(defn begin
  "
  Open a new transaction. Possible arguments:

  - `tx-level`: the custom isolation level, either a `:kebab-case`
     keyword, or a CAPS STRING, for example `:read-committed`
     or `'READ COMMITTED'`;
  - `read-only?`: whether to run the transaction in read-only mode
    (default is false).
  "
  ([^Connection conn]
   (.begin conn))

  ([^Connection conn tx-level]
   (.begin conn (->tx-level tx-level)))

  ([^Connection conn tx-level read-only?]
   (.begin conn (->tx-level tx-level) (boolean read-only?))))


(defn commit
  "
  Commit the current transaction.
  "
  [^Connection conn]
  (.commit conn))


(defn rollback
  "
  Rollback the current transaction.
  "
  [^Connection conn]
  (.rollback conn))


(defn set-tx-level
  "
  Set transaction isolation level for the current transaction.
  "
  [^Connection conn level]
  (.setTxLevel conn (->tx-level level)))


(defn set-read-only
  "
  Set the current transaction read-only.
  "
  [^Connection conn]
  (.setTxReadOnly conn))


(def TX_DEFAULTS
  {:isolation-level nil
   :read-only? false
   :rollback? false})


(defmacro with-transaction
  "
  Obtain a connection from a source and perform a block of code
  wrapping the connection with a transaction, namely:

  - run BEGIN before the code block;
  - capture all possible exceptions;
  - should an exception was caught, ROLLBACK...
  - and re-throw it;
  - if no exception was caught, COMMIT.

  Arguments:
  - `tx` is a symbol which a transactional connection is bound to;
  - `src` is a data source (a map, a Pool, a Connection, a URI string).

  The third argument is an optional map of parameters:

  - `isolation-level`: a keyword/string to set the isolation level;
  - `read-only?`: to set the transaction read only;
  - `rollback?`: to ROLLBACK a transaction even if it was successful.

  Nested transactions are consumed by the most outer transaction.
  For example, you have two nested `with-transaction` blocks:

  (with-transaction [tx1 {...}]          ;; 1
    (do-this ...)
    (with-transaction [tx2 tx1]          ;; 2
      (do-that ...)))

  In this case, only the first block will produce `BEGIN` and `COMMIT`
  commands. The second block will expand into the body only:

  (pg/begin ...)
    (do-this ...)
    (do-that ...)
  (pg/commit ...)

  "
  {:arglists '([[tx src] & body]
               [[tx src {:keys [isolation-level
                                read-only?
                                rollback?]}] & body])}
  [[tx src opts] & body]

  `(with-conn [~tx ~src]
     (if (in-transaction? ~tx)
       (do ~@body)
       (let [opts#
             ~(if opts
                `(merge TX_DEFAULTS ~opts)
                `TX_DEFAULTS)

             {iso-level# :isolation-level
              read-only?# :read-only?
              rollback?# :rollback?}
             opts#]

         (with-lock [~tx]
           (begin ~tx iso-level# read-only?#)
           (try
             (let [result# (do ~@body)]
               (if rollback?#
                 (.rollback ~tx)
                 (.commit ~tx))
               result#)
             (catch Throwable e#
               (.rollback ~tx)
               (throw e#))))))))


(defn connection?
  "
  True of the passed option is a Connection instance.
  "
  [x]
  (instance? Connection x))


;;
;; Prints
;;

(defmethod print-method Connection
  [^Connection conn ^Writer writer]
  (.write writer (.toString conn)))


(defmethod print-method Pool
  [^Pool pool ^Writer writer]
  (.write writer (.toString pool)))


;;
;; Listen/notify block
;;

(defn listen
  "
  Subscribe a connection to a given channel.
  "
  [^Connection conn ^String channel]
  (.listen conn channel))


(defn unlisten
  "
  Unsubscribe a connection from a given channel.
  "
  [^Connection conn ^String channel]
  (.unlisten conn channel))


(defn notify
  "
  Send a text message to a given channel.
  "
  [src ^String channel ^String message]
  (with-conn [conn src]
    (.notify conn channel message)))


(defn notifications?
  "Returns true when the connection has received notifications that have
  not yet be used. Drain them with `drain-notifications`.
  "
  [^Connection conn]
  (.hasNotifications conn))

(defn drain-notifications
  "Fetch and drain all received notifications from the connection. This
  is a destructive action in that the messages will be removed from
  the underlying connection.
  "
  [^Connection conn]
  (.drainNotifications conn))


;;
;; JSON
;;

(defn json-wrap
  "
  Wrap a value into a JSON.Wrapper class to force JSON encoding.
  "
  ^JSON$Wrapper [x]
  (JSON/wrap x))


;;
;; Encode/decode
;;

(defn ->codec-params ^CodecParams [^Map opt]

  (let [{:keys [^String client-charset
                ^String server-charset
                ^String date-style
                ^String time-zone-id
                ^Boolean integer-datetime?
                ^ObjectMapper object-mapper]}
        opt]

    (if-not (seq opt)

      CodecParams/STANDARD

      (cond-> (CodecParams/builder)

        client-charset
        (as-> builder
            (let [charset (Charset/forName client-charset)]
              (.clientCharset builder charset)))

        server-charset
        (as-> builder
            (let [charset (Charset/forName server-charset)]
              (.serverCharset builder charset)))

        date-style
        (.dateStyle date-style)

        time-zone-id
        (as-> builder
            (let [zone-id (ZoneId/of time-zone-id)]
              (.timeZone builder zone-id)))

        (some? integer-datetime?)
        (.integerDatetime integer-datetime?)

        object-mapper
        (.objectMapper object-mapper)

        :finally
        (.build)))))


(defn- -get-processor ^IProcessor [oid]
  (or (Processors/getProcessor oid)
      Processors/unsupported))


(defn decode-bin
  "
  Decode a binary-encoded value from a ByteBuffer.
  "
  ([^ByteBuffer buf oid]
   (decode-bin buf oid nil))

  ([^ByteBuffer buf oid opt]
   (.rewind buf)
   (let [processor (-get-processor oid)]
     (.decodeBin processor buf (->codec-params opt)))))


(defn decode-txt
  "
  Decode a text-encoded value from a ByteBuffer.
  "
  ([^String obj oid]
   (decode-txt obj oid nil))

  ([^String obj oid opt]
   (let [processor (-get-processor oid)]
     (.decodeTxt processor obj (->codec-params opt)))))


(defn encode-bin
  "
  Binary-encode a value into a ByteBuffer.
  "
  (^ByteBuffer [obj]
   (encode-bin obj nil nil))

  (^ByteBuffer [obj oid]
   (encode-bin obj oid nil))

  (^ByteBuffer [obj oid opt]
   (let [oid
         (or oid (OID/defaultOID obj))
         processor
         (-get-processor oid)]
     (.encodeBin processor obj (->codec-params opt)))))


(defn encode-txt
  "
  Text-encode a value into a string.
  "
  (^String [obj]
   (encode-txt obj nil nil))

  (^String [obj oid]
   (encode-txt obj oid nil))

  (^String [obj oid opt]
   (let [oid
         (or oid (OID/defaultOID obj))
         processor
         (-get-processor oid)]
     (.encodeTxt processor obj (->codec-params opt)))))


;;
;; SSL
;;

(defn is-ssl?
  "
  True if the Connection is SSL-encrypted.
  "
  ^Boolean [^Connection conn]
  (.isSSL conn))


;;
;; Pool?
;;

(defn pool?
  "
  True if a value is a Pool instance.
  "
  [x]
  (instance? Pool x))


(defn pool
  "
  Run a new Pool from a config map or a URI.
  "
  (^Pool [src]
   (-> src
       (src/-to-config)
       (Pool/create)))

  (^Pool [^String host ^Integer port ^String user ^String password ^String database]
   (Pool/create host port user password database)))


(defmacro with-pool
  "
  Execute the body while the `bind` symbol is bound
  to a new Pool instance. Close the pool afterwards.
  "
  [[bind config] & body]
  `(with-open [~bind (pool ~config)]
     ~@body))


;;
;; THE ABYSS OF DEPRECATED
;;

(defn ^:deprecated get-error-fields
  "
  Get a map of error fields from an instance of
  `PGErrorResponse`. **Deprecated**, use `ex-data`
  instead.
  "
  [^PGErrorResponse e]
  (ex-data e))


(defmacro ^:deprecated with-tx
  "
  **DEPRECATED**: use `with-transaction` above.
  ---------------------------------------------
  Acts like `with-transaction` but accepts a connection,
  not a data source. Thus, no a binding symbol required.
  "
  [[conn opts] & body]
  `(with-transaction [_# ~conn ~opts]
     ~@body))
