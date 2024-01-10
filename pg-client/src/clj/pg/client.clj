(ns pg.client
  "
  Common API to communicate with PostgreSQL server.
  "
  (:require
   [clojure.string :as str]
   [less.awful.ssl :as ssl])
  (:import
   clojure.lang.Keyword
   java.io.InputStream
   java.io.OutputStream
   java.io.Writer
   java.lang.System$Logger$Level
   java.nio.ByteBuffer
   java.util.List
   java.util.Map
   java.util.UUID
   javax.net.ssl.SSLContext
   org.pg.CancelTimer
   org.pg.ConnConfig$Builder
   org.pg.Connection
   org.pg.ExecuteParams
   org.pg.ExecuteParams$Builder
   org.pg.PreparedStatement
   org.pg.codec.DecoderBin
   org.pg.codec.DecoderTxt
   org.pg.codec.EncoderBin
   org.pg.codec.EncoderTxt
   org.pg.enums.CopyFormat
   org.pg.enums.OID
   org.pg.enums.TXStatus
   org.pg.enums.TxLevel
   org.pg.reducer.IReducer
   org.pg.type.JSON
   org.pg.type.JSON$Wrapper
   org.pg.type.PGEnum))


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

                ;; reducers
                reducer
                group-by
                index-by
                index-by-id?
                matrix?
                java?
                fold
                init
                run
                kv
                first?

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

      reducer
      (.reducer reducer)

      max-rows
      (.maxRows max-rows)

      fn-key
      (.fnKeyTransform fn-key)

      output-stream
      (.outputStream output-stream)

      input-stream
      (.inputStream input-stream)

      group-by
      (.groupBy group-by)

      index-by
      (.indexBy index-by)

      index-by-id?
      (.indexBy :id)

      run
      (.run run)

      matrix?
      (.asMatrix)

      java?
      (.asJava)

      kebab-keys?
      (.fnKeyTransform ->kebab)

      kv
      (.KV (first kv) (second kv))

      first?
      (.first)

      (and fold init)
      (.fold fold init)

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
      (.copyMapKeys copy-in-keys)

      :finally
      (.build))))


(defn ->Level
  "
  Turn a keyword into an instance of System.Logger.Level enum.
  "
  ^System$Logger$Level [^Keyword log-level]
  (case log-level

    :all
    System$Logger$Level/ALL

    :trace
    System$Logger$Level/TRACE

    :debug
    System$Logger$Level/DEBUG

    :info
    System$Logger$Level/INFO

    (:warn :warning)
    System$Logger$Level/WARNING

    :error
    System$Logger$Level/ERROR

    (:off false nil)
    System$Logger$Level/OFF))


(defn ->conn-config
  "
  Turn a Clojure map into an instance of ConnConfig.Builder.
  "
  ^ConnConfig$Builder [params]

  (let [{:keys [;; general
                user
                database
                host
                port
                password
                pg-params

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

                ;; misc
                ms-cancel-timeout
                protocol-version]}
        params]

    (cond-> (new ConnConfig$Builder user database)

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

      log-level
      (.logLevel (->Level log-level))

      ms-cancel-timeout
      (.msCancelTimeout ms-cancel-timeout)

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
   (new Connection (->conn-config config)))

  (^Connection [^String host ^Integer port ^String user ^String password ^String database]
   (new Connection host port user password database)))


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
   [^Connection conn ^String sql ^List oids]
   (.prepare conn sql oids)))


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


(defn begin
  "
  Open a new transaction.
  "
  [^Connection conn]
  (.begin conn))


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

  "
  [[conn {:as opt :keys [isolation-level
                         read-only?
                         rollback?]}]
   & body]

  (let [bind (gensym "CONN")]

    `(let [~bind ~conn]

       (.begin ~bind)

       ~@(when isolation-level
           [`(set-tx-level ~bind ~isolation-level)])

       ~@(when read-only?
           [`(set-read-only ~bind)])

       (try
         (let [result# (do ~@body)]
           ~(if rollback?
              `(.rollback ~bind)
              `(.commit ~bind))
           result#)
         (catch Throwable e#
           (.rollback ~bind)
           (throw e#))))))


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
  (new JSON$Wrapper x))


(defn json-read-string
  "
  Parse JSON from a string.
  "
  [^String input]
  (JSON/readValue input))


(defn json-write-writer
  "
  Parse JSON from a Writer instance.
  "
  [value ^Writer writer]
  (JSON/writeValue writer value))


(defn json-write-stream
  "
  Encode JSON into the OutputStream.
  "
  [value ^OutputStream out]
  (JSON/writeValue out value))


(defn json-write-string
  "
  Encode JSON into a string.
  "
  ^String [value]
  (JSON/writeValueToString value))


;;
;; Encode/decode
;;

(defn decode-bin
  "
  Decode a binary-encoded value from a ByteBuffer.
  "
  [^ByteBuffer buf ^OID oid]
  (.rewind buf)
  (DecoderBin/decode buf oid))


(defn decode-txt
  "
  Decode a text-encoded value from a ByteBuffer.
  "
  [^String obj ^OID oid]
  (DecoderTxt/decode obj oid))


(defn encode-bin
  "
  Binary-encode a value into a ByteBuffer.
  "
  (^ByteBuffer [obj]
   (EncoderBin/encode obj))

  (^ByteBuffer [obj ^OID oid]
   (EncoderBin/encode obj oid)))


(defn encode-txt
  "
  Text-encode a value into a ByteBuffer.
  "
  (^String [obj]
   (EncoderTxt/encode obj))

  (^String [obj ^OID oid]
   (EncoderTxt/encode obj oid)))


;;
;; Enum
;;

(defn ->enum
  "
  Wrap a value with a PGEnum class for proper enum encoding
  "
  ^PGEnum [x]
  (PGEnum/of x))


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
