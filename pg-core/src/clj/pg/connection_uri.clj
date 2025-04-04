(ns pg.connection-uri
  "
  A namespace to parse a connection URI string into
  a map of config fields.

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

(defn parse-ref
  "
  Resolve a Clojure object by a fully qualified string
  pointing on it (like 'org.acme.util/my-handerl').
  Throw an error should a string is not qualified or
  point to a missing object.
  "
  [^String line]
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
  Arguments with dots are treated as nested maps, e.g.:

  name=test&params.id=123 ->
  {'name' 'test', 'params' {'id' '123'}}
  "
  [^String line]
  (when-not (str/blank? line)
    (let [key=vals
          (str/split (str/trim line) #"&")]

      (->> key=vals
           (map
            (fn [key=val]
              (str/split key=val #"=" 2)))
           (remove
            (fn [[_key val]]
              (or (= val "") (= val nil))))
           (map
            (fn [[key val]]
              [(str/split key #"\.") val]))
           (reduce
            (fn [acc [path val]]
              (assoc-in acc path val))
            nil)))))

(defn parse
  "
  Parse a string URI into a map of Config options.
  See `pg.config/->config`.
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
                pg-params

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
                ssl-context
                ;; TODO: ssl certs for ssl context?

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
                read-pg-types

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

     :ssl-context
     (some-> ssl-context parse-ref)

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

     ;; types
     ;; TODO type-map
     :read-pg-types?
     (some-> read-pg-types parse-bool)

     :pg-params
     (cond-> pg-params
       ApplicationName
       (assoc "application_name" ApplicationName))}))
