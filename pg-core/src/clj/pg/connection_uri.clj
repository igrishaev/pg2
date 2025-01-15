(ns pg.connection-uri
  "
  Links:
  - https://jdbc.postgresql.org/documentation/use/
  "
  (:refer-clojure :exclude [parse-long])
  (:require
   [clojure.string :as str])
  (:import
   (java.net URI)))

(set! *warn-on-reflection* true)

(defmacro error! [template & args]
  `(throw (new Exception (format ~template ~@args))))

(defn parse-bool ^Boolean [^String line]
  (case (str/lower-case line)
    ("true" "on" "1" "yes") true
    ("false" "off" "0" "no") false
    (error! "cannot parse boolean value: %s" line)))

(defn parse-long ^Long [^String line]
  (try
    (Long/parseLong line)
    (catch Exception e
      (error! "cannot parse long value: %s, reason: %s"
        line (ex-message e)))))

(defn parse-ref [^String line]
  (try
    (-> line
        symbol
        requiring-resolve
        (or (error! "reference not found: %s" line))
        deref)
    (catch Exception e
      (error! "cannot resolve a reference: %s, reason: %s"
        line (ex-message e)))))

(defn parse-query-string
  "
  Parse a decoded query string line into a Clojure map.
  Empty string values are turned into nil, e.g:
  foo=&bar=42 -> {:foo nil :bar '42'}. Throw an exception
  in some incorrect cases.
  "
  [^String line]
  (when-not (str/blank? line)
    (let [query-args
          (as-> (str/trim line) *
            (str/split * #"&")
            (mapcat #(str/split % #"=" 2) *))]

      (when-not (-> query-args count even?)
        (error! "malformed query params: %s" line))

      (->> query-args
           (replace {"" nil})
           (apply hash-map)))))

(defn parse
  "
  Parse a string URI into a map of Config options.
  See `pg.core/->config`.
  "
  [^String connection-uri]
  (let [connection-uri
        (str/replace-first connection-uri #"^jdbc:" "")

        uri
        (new URI connection-uri)

        host
        (.getHost uri)

        port
        (.getPort uri)

        port
        (when (pos? port) port)

        user-info
        (.getUserInfo uri)

        path
        (str (.getPath uri))

        database
        (cond-> path
          (str/starts-with? path "/")
          (subs 1))

        query-params
        (some-> uri .getQuery parse-query-string)

        [user password]
        (when user-info
          (str/split user-info #":" 2))

        user
        (or (get query-params "user")
            (not-empty user))

        password
        (or (get query-params "password")
            (not-empty password))

        {:strs [read-only
                ;; TODO :pg-params

                ;; socket options
                so-keep-alive
                so-tcp-no-delay
                so-timeout
                so-recv-buf-size
                so-send-buf-size

                ;; bin/text
                binary-encode
                binary-decode

                ;; streams
                in-stream-buf-size
                out-stream-buf-size

                ;; ssl
                ssl
                ;; TODO: ssl certs for ssl context?
                ;; TODO: ssl-context ref?

                fn-notification
                fn-protocol-version
                fn-notice

                ;; TODO: unix:// or file:// scheme for unix socket?
                ;; unix-socket
                ;; unix-socket-path

                ;; misc
                cancel-timeout-ms
                protocol-version

                ;; json
                object-mapper

                ;; pool
                pool-min-size
                pool-max-size
                pool-expire-threshold-ms
                pool-borrow-conn-timeout-ms

                ;; types
                with-pgvector

                ;; JDBC camelCase options
                readOnly
                connectTimeout
                ApplicationName
                cancelSignalTimeout
                binaryTransfer
                tcpKeepAlive
                tcpNoDelay
                protocolVersion]}
        query-params]

    {;; general
     :user user
     :database database
     :host host
     :port port
     :password password

     ;; Next.JDBC
     :dbname database

     ;; enc/dec format
     :binary-encode?
     (some-> (or binaryTransfer binary-encode) parse-bool)

     :binary-decode?
     (some-> (or binary-decode binaryTransfer) parse-bool)

     ;; copy in/out
     :in-stream-buf-size
     (some-> in-stream-buf-size parse-long)

     :out-stream-buf-size
     (some-> out-stream-buf-size parse-long)

     ;; handlers

     :fn-notification
     (some-> fn-notification parse-ref)

     :fn-protocol-version
     (some-> fn-protocol-version parse-ref)

     :fn-notice
     (some-> fn-notice parse-ref)

     ;; ssl
     :use-ssl?
     (some-> ssl parse-bool)

     ;; TODO unix domain socket
     ;; :unix-socket?
     ;; :unix-socket-path

     ;; socket
     :so-keep-alive?
     (some-> (or so-keep-alive tcpKeepAlive) parse-bool)

     :so-tcp-no-delay?
     (some-> (or so-tcp-no-delay tcpNoDelay) parse-bool)

     :so-timeout
     (some-> (or so-timeout connectTimeout) parse-long)

     :so-recv-buf-size
     (some-> so-recv-buf-size parse-long)

     :so-send-buf-size
     (some-> so-send-buf-size parse-long)

     ;; TODO logging
     ;; :log-level

     ;; json
     :object-mapper
     (some-> object-mapper parse-ref)

     ;; read only

     :read-only?
     (some-> (or read-only readOnly) parse-bool)

     ;; misc

     :cancel-timeout-ms
     (some-> (or cancel-timeout-ms cancelSignalTimeout) parse-long)

     :protocol-version
     (some-> (or protocol-version protocolVersion) parse-long)

     ;; pool

     :pool-borrow-conn-timeout-ms
     (some-> pool-borrow-conn-timeout-ms parse-long)

     :pool-expire-threshold-ms
     (some-> pool-expire-threshold-ms parse-long)

     :pool-max-size
     (some-> pool-max-size parse-long)

     :pool-min-size
     (some-> pool-min-size parse-long)

     ;; TODO types
     ;; :type-map
     ;; :enums

     :with-pgvector?
     (some-> with-pgvector parse-bool)

     ;; TODO parse pg-params
     :pg-params
     (cond-> nil
       ApplicationName
       (assoc "application_name" ApplicationName))}))
