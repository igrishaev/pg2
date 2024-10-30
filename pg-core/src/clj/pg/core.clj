(ns pg.core
  "
  Common API to communicate with PostgreSQL server.
  "
  (:require
   [clojure.string :as str]
   [less.awful.ssl :as ssl]
   [pg.fold :as fold]
   [pg.oid :as oid])
  (:import
   clojure.lang.IDeref
   clojure.lang.IPersistentMap
   clojure.lang.Keyword
   com.fasterxml.jackson.databind.ObjectMapper
   java.io.InputStream
   java.io.OutputStream
   java.io.Reader
   java.io.Writer
   java.lang.AutoCloseable
   java.nio.ByteBuffer
   java.nio.charset.Charset
   java.time.ZoneId
   java.util.List
   java.util.Map
   java.util.UUID
   javax.net.ssl.SSLContext
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
   org.pg.processor.IProcessor
   org.pg.processor.Processors
   org.pg.enums.OID
   org.pg.enums.CopyFormat
   org.pg.enums.TXStatus
   org.pg.enums.TxLevel
   org.pg.error.PGError
   org.pg.error.PGErrorResponse
   org.pg.json.JSON
   org.pg.json.JSON$Wrapper))


(def ^CopyFormat COPY_FORMAT_BIN CopyFormat/BIN)
(def ^CopyFormat COPY_FORMAT_CSV CopyFormat/CSV)
(def ^CopyFormat COPY_FORMAT_TAB CopyFormat/TAB)


(defn ->kebab
  "
  Turn a string column name into into  a kebab-case
  formatted keyword.
  "
  ^Keyword [^String column]
  (-> column (str/replace #"_" "-") keyword))


;;
;; Errors
;;

(defn get-error-fields [^PGErrorResponse e]
  (when (instance? PGErrorResponse e)
    (.getErrorFields e)))


(defn error! [template & args]
  (throw (new PGError (apply format template args))))


;;
;; Config builders
;;

(defn ->execute-params
  "
  Make an instance of ExecuteParams from a Clojure map.
  "
  ^ExecuteParams [^Map opt]

  (let [{:keys [^List params
                oids

                ;; portal
                max-rows

                ;; keys
                kebab-keys?
                fn-key

                ;; streams
                output-stream
                input-stream

                ;; fold/reduce
                as
                first
                first? ;; for backward compatibility
                map
                index-by
                group-by
                kv
                java
                run
                column
                columns
                table
                to-edn
                to-json
                reduce
                into

                ;; format
                binary-encode?
                binary-decode?

                ;; copy csv
                csv-null
                csv-sep
                csv-end

                ;; copy general
                copy-buf-size
                ^CopyFormat copy-format
                copy-csv?
                copy-bin?
                copy-tab?
                copy-in-rows
                copy-in-maps
                copy-in-keys]}
        opt]

    (cond-> (ExecuteParams/builder)

      params
      (.params params)

      oids
      (.OIDs oids)

      max-rows
      (.maxRows max-rows)

      fn-key
      (.fnKeyTransform fn-key)

      output-stream
      (.outputStream output-stream)

      input-stream
      (.inputStream input-stream)

      kebab-keys?
      (.fnKeyTransform ->kebab)

      ;;
      ;; reducers
      ;;
      as
      (.reducer as)

      (or first first?)
      (.reducer fold/first)

      map
      (.reducer (fold/map map))

      index-by
      (.reducer (fold/index-by index-by))

      group-by
      (.reducer (fold/group-by group-by))

      kv
      (.reducer (fold/kv (clojure.core/first kv) (second kv)))

      run
      (.reducer (fold/run run))

      column
      (.reducer (fold/column column))

      columns
      (.reducer (fold/columns columns))

      table
      (.reducer (fold/table))

      java
      (.reducer fold/java)

      to-edn
      (.reducer (fold/to-edn to-edn))

      to-json
      (.reducer (fold/to-json to-json))

      reduce
      (.reducer (fold/reduce (clojure.core/first reduce)
                             (second reduce)))

      into
      (.reducer (fold/into (clojure.core/first into)
                           (second into)))

      ;;

      (some? binary-encode?)
      (.binaryEncode binary-encode?)

      (some? binary-decode?)
      (.binaryDecode binary-decode?)

      csv-null
      (.CSVNull csv-null)

      csv-sep
      (.CSVCellSep csv-sep)

      csv-end
      (.CSVLineSep csv-end)

      oids
      (.OIDs oids)

      copy-csv?
      (.setCSV)

      copy-bin?
      (.setBin)

      copy-tab?
      (.setBin)

      copy-format
      (.copyFormat copy-format)

      copy-buf-size
      (.copyBufSize copy-buf-size)

      copy-in-rows
      (.copyInRows copy-in-rows)

      copy-in-maps
      (.copyInMaps copy-in-maps)

      copy-in-keys
      (.copyInKeys copy-in-keys)

      :finally
      (.build))))


(defn ->config
  "
  Turn a Clojure map into an instance of Config.Builder.
  "
  ^Config$Builder [params]

  (let [{:keys [;; general
                user
                database
                host
                port
                password
                pg-params

                ;; Next.JDBC
                dbname

                ;; enc/dec format
                binary-encode?
                binary-decode?

                ;; copy in/out
                in-stream-buf-size
                out-stream-buf-size

                ;; handlers
                fn-notification
                fn-protocol-version
                fn-notice

                ;; ssl
                use-ssl?
                ssl-context

                ;; socket
                so-keep-alive?
                so-tcp-no-delay?
                so-timeout
                so-recv-buf-size
                so-send-buf-size

                ;; logging
                log-level

                ;; json
                ^ObjectMapper object-mapper

                ;; read only
                read-only?

                ;; misc
                cancel-timeout-ms
                protocol-version

                ;; pool
                pool-min-size
                pool-max-size
                pool-expire-threshold-ms
                pool-borrow-conn-timeout-ms

                ;; types
                type-map
                enums

                with-pgvector?]}
        params

        DB
        (or database dbname)]

    (cond-> (new Config$Builder user DB)

      password
      (.password password)

      host
      (.host host)

      port
      (.port port)

      protocol-version
      (.protocolVersion protocol-version)

      pg-params
      (.pgParams pg-params)

      (some? binary-encode?)
      (.binaryEncode binary-encode?)

      (some? binary-decode?)
      (.binaryDecode binary-decode?)

      in-stream-buf-size
      (.inStreamBufSize in-stream-buf-size)

      out-stream-buf-size
      (.outStreamBufSize out-stream-buf-size)

      (some? use-ssl?)
      (.useSSL use-ssl?)

      ssl-context
      (.sslContext ssl-context)

      fn-notification
      (.fnNotification fn-notification)

      fn-protocol-version
      (.fnProtocolVersion fn-protocol-version)

      fn-notice
      (.fnNotice fn-notice)

      (some? so-keep-alive?)
      (.SOKeepAlive so-keep-alive?)

      (some? so-tcp-no-delay?)
      (.SOTCPnoDelay so-tcp-no-delay?)

      so-timeout
      (.SOTimeout so-timeout)

      so-recv-buf-size
      (.SOReceiveBufSize so-recv-buf-size)

      so-send-buf-size
      (.SOSendBufSize so-send-buf-size)

      object-mapper
      (.objectMapper object-mapper)

      cancel-timeout-ms
      (.cancelTimeoutMs cancel-timeout-ms)

      read-only?
      (.readOnly)

      pool-min-size
      (.poolMinSize pool-min-size)

      pool-max-size
      (.poolMaxSize pool-max-size)

      pool-expire-threshold-ms
      (.poolExpireThresholdMs pool-expire-threshold-ms)

      pool-borrow-conn-timeout-ms
      (.poolBorrowConnTimeoutMs pool-borrow-conn-timeout-ms)

      type-map
      (.typeMap type-map)

      enums
      (.enums enums)

      with-pgvector?
      (.usePGVector)

      :finally
      (.build))))


(defn connect

  "
  Connect to the database. Given a Clojure config map,
  establish a TCP connection with the server and run
  the authentication pipeline. Returns an instance of
  the Connection class.
  "

  (^Connection [config]
   (Connection/connect (->config config)))

  (^Connection [^String host ^Integer port ^String user ^String password ^String database]
   (Connection/connect host port user password database)))


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


(defmacro with-lock [[conn] & body]
  `(with-open [_# (.getLock ~conn)]
     ~@body))


(defn idle?
  "
  True if the connection is in the idle state.
  "
  ^Boolean [^Connection conn]
  (.isIdle conn))


(defn in-transaction?
  "
  True if the connection is in transaction at the moment.
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


(defn id
  "
  Get a unique ID of the connection.
  "
  ^UUID [^Connection conn]
  (.getId conn))


(defn pid
  "
  Get PID of the connection on the server.
  "
  ^Integer [^Connection conn]
  (.getPid conn))


(defn created-at
  "
  Get the creation time as Unix timestamp (ms).
  "
  ^Long [^Connection conn]
  (.getCreatedAt conn))


(defn close-statement
  "
  Close the prepared statement.
  "
  [^Connection conn ^PreparedStatement stmt]
  (.closeStatement conn stmt))


(defn close
  "
  Close the connection to the database.
  "
  [^Connection conn]
  (.close conn))


(defn ssl?
  "
  True if the connection is encrypted with SSL.
  "
  ^Boolean [^Connection conn]
  (.isSSL conn))


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
  "

  (^PreparedStatement
   [^Connection conn ^String sql]
   (.prepare conn sql ExecuteParams/INSTANCE))

  (^PreparedStatement
   [^Connection conn ^String sql ^Map opt]
   (.prepare conn sql (->execute-params opt))))


(defn execute-statement

  "
  Execute the given prepared statement and get the result.
  The way the result is processed heavily depends on the options.
  "

  ([^Connection conn ^PreparedStatement stmt]
   (.executeStatement conn stmt ExecuteParams/INSTANCE))

  ([^Connection conn ^PreparedStatement stmt ^Map opt]
   (.executeStatement conn stmt (->execute-params opt))))


(defn execute

  "
  Execute a SQL expression and return the result.

  This function is a series of steps:
  - prepare a statement;
  - bind parameters and obtain a portal;
  - describe the portal;
  - get the data from the portal;
  - close the portal;
  - close the statement;
  - process the result of the fly.
  "

  ([^Connection conn ^String sql]
   (.execute conn sql ExecuteParams/INSTANCE))

  ([^Connection conn ^String sql ^Map opt]
   (.execute conn sql (->execute-params opt))))


(defmacro with-timeout
  "
  Set a timeout in which, if a query has not been executed in time,
  a cancellation request is sent to the server. Wrap a query with
  that macro when there is a chance for a server to freeze completely
  due to a non-optimized SQL. The `ms-timeout` is amount of milliseconds
  in which the cancel request will be sent.
  "
  [[conn ms-timeout] & body]
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


(defmacro with-connection
  "
  Execute the body while the `bind` symbol is bound to the
  new Connection instance. The connection gets closed when
  exiting the macro.
  "
  [[bind config] & body]
  `(let [~bind (connect ~config)]
     (try
       ~@body
       (finally
         (close ~bind)))))


(defmacro with-conn
  "
  Just a shorter version of `with-connection`.
  "
  [[bind config] & body]
  `(with-connection [~bind ~config]
     ~@body))


(defn closed?
  "
  True if the connection has been closed.
  "
  [^Connection conn]
  (.isClosed conn))


(defn query

  "
  Run a SQL expression WITH NO parameters. The result
  gets sent back in text mode always by the server.
  "

  ([^Connection conn ^String sql]
   (.query conn sql ExecuteParams/INSTANCE))

  ([^Connection conn ^String sql ^Map opt]
   (.query conn sql (->execute-params opt))))


(defn clone
  "
  Create a new Connection from a configuration of the given connection.
  "
  ^Connection [^Connection conn]
  (Connection/clone conn))


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
  ([^Connection conn ^String sql ^OutputStream out]
   (copy-out conn sql out nil))

  ([^Connection conn ^String sql ^OutputStream out ^Map opt]
   (.copy conn
          sql
          (-> opt
              (assoc :output-stream out)
              (->execute-params)))))


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

  ([^Connection conn ^String sql ^InputStream in]
   (copy-in conn sql in nil))

  ([^Connection conn ^String sql ^InputStream in ^Map opt]
   (.copy conn
          sql
          (-> opt
              (assoc :input-stream in)
              (->execute-params)))))


(defn copy-in-rows

  "
  Like `copy-in` but accepts not an input stream but a list
  of rows. Each row must be a list of values. The list might be
  lazy. Return a number of rows processed.
  "

  ([^Connection conn ^String sql ^List rows]
   (copy-in-rows conn sql rows nil))

  ([^Connection conn ^String sql ^List rows ^Map opt]
   (.copy conn
          sql
          (-> opt
              (assoc :copy-in-rows rows)
              (->execute-params)))))


(defn copy-in-maps

  "
  Like `copy-in` but accepts a list of Clojure maps.

  The `keys` argument is list of keys which is used to convert
  each map into a tuple.

  The `opt` argument is a map of options (COPY format, delimiters, etc).

  Return the number of rows processed by the server.
  "

  ([^Connection conn ^String sql ^List maps ^List keys]
   (copy-in-maps conn sql maps keys nil))

  ([^Connection conn ^String sql ^List maps ^List keys ^Map opt]
   (.copy conn
          sql
          (-> opt
              (assoc :copy-in-maps maps
                     :copy-in-keys keys)
              (->execute-params)))))


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
    TxLevel/READ_UNCOMMITTED))


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


(defmacro with-tx
  "
  Wrap a block of code into a transaction, namely:

  - run BEGIN before executing the code;
  - capture all possible exceptions;
  - should an exception was caught, ROLLBACK...
  - and re-throw it;
  - if no exception was caught, COMMIT.

  Accepts a map of the following options:

  - isolation-level: a keyword/string to set the isolation level;
  - read-only?: to set the transaction read only;
  - rollback?: to ROLLBACK a transaction even if it was successful.

  Nested transactions are consumed by the most outer transaction.
  For example, you have two nested `with-tx` blocks:

  (with-tx [...]          ;; 1
    (do-this ...)
    (with-tx [...]        ;; 2
      (do-that ...)))

  In this case, only the first block will produce `BEGIN` and `COMMIT`
  commands. The second block will expand into the body only:

  (pg/begin ...)
    (do-this ...)
    (do-that ...)
  (pg/commit ...)

  "
  {:arglists '([[conn] & body]
               [[conn {:keys [isolation-level
                              read-only?
                              rollback?]}] & body])}
  [[conn opts] & body]

  (let [CONN
        (with-meta (gensym "CONN")
          {:tag `Connection})

        OPTS
        (gensym "OPTS")]

    `(if (in-transaction? ~conn)

       (do ~@body)

       (let [~CONN ~conn
             ~OPTS ~opts

             iso-level#
             ~(if opts
                `(or (some-> ~OPTS :isolation-level ->tx-level)
                     TxLevel/NONE)
                `TxLevel/NONE)

             read-only?#
             ~(if opts
                `(boolean (:read-only? ~OPTS))
                `false)

             rollback?#
             ~(if opts
                `(boolean (:rollback? ~OPTS))
                `false)]

         (with-lock [~CONN]

           (.begin ~CONN iso-level# read-only?#)

           (try
             (let [result# (do ~@body)]
               (if rollback?#
                 (.rollback ~CONN)
                 (.commit ~CONN))
               result#)
             (catch Throwable e#
               (.rollback ~CONN)
               (throw e#))))))))


(defn connection?
  "
  True of the passed option is a Connection instance.
  "
  [x]
  (instance? Connection x))


(defmethod print-method Connection
  [^Connection conn ^Writer writer]
  (.write writer (.toString conn)))


(defn listen
  "
  Subscribe the connection to a given channel.
  "
  [^Connection conn ^String channel]
  (.listen conn channel))


(defn unlisten
  "
  Unsubscribe the connection from a given channel.
  "
  [^Connection conn ^String channel]
  (.unlisten conn channel))


(defn notify
  "
  Send a text message to the given channel.
  "
  [^Connection conn ^String channel ^String message]
  (.notify conn channel message))


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

(defn ssl-context
  "
  Build a SSLContext instance from a Clojure map.
  All the keys point to local files.
  "
  ^SSLContext [{:keys [^String key-file
                       ^String cert-file
                       ^String ca-cert-file]}]
  (if ca-cert-file
    (ssl/ssl-context key-file cert-file ca-cert-file)
    (ssl/ssl-context key-file cert-file)))


(defn is-ssl?
  "
  True if the Connection is SSL-encrypted.
  "
  ^Boolean [^Connection conn]
  (.isSSL conn))


(defn ssl-context-reader [mapping]
  `(ssl-context ~mapping))


;;
;; Connectable source abstraction
;;

(defprotocol IConnectable

  (-borrow-connection [this])

  (-return-connection [this conn]))


(extend-protocol IConnectable

  Connection

  (-borrow-connection [^Connection this]
    this)

  (-return-connection [this ^Connection conn]
    nil)

  Pool

  (-borrow-connection [this]
    (.borrowConnection this))

  (-return-connection [this ^Connection conn]
    (.returnConnection this conn))

  IPersistentMap

  (-borrow-connection [this]
    (connect this))

  (-return-connection [this ^Connection conn]
    (close conn))

  Config

  (-borrow-connection [this]
    (Connection/connect this))

  (-return-connection [this ^Connection conn]
    (close conn))

  Object

  (-borrow-connection [this]
    (error! "Unsupported connection source: %s" this))

  (-return-connection [this ^Connection conn]
    (error! "Unsupported connection source: %s" this))

  nil

  (-borrow-connection [this]
    (error! "Connection source cannot be null"))

  (-return-connection [this ^Connection conn]
    (error! "Connection source cannot be null")))


(defmacro on-connection
  "
  Perform a block of code while the `bind` symbol is bound
  to a `Connection` object. If the source is a config map,
  the connection gets closed. When the source is a pool,
  the connection gets borrowed and returned afterwards.
  For existing connection, nothing is happening at the end.
  "
  [[bind source] & body]
  `(let [source#
         ~source
         ~(with-meta bind {:tag `Connection})
         (-borrow-connection source#)]
     (try
       ~@body
       (finally
         (-return-connection source# ~bind)))))
