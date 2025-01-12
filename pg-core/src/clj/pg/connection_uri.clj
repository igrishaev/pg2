(ns pg.connection-uri
  (:require
   [clojure.string :as str])
  (:import
   (java.net URI)))

(set! *warn-on-reflection* true)

(defmacro error! [template & args]
  `(throw (new Exception (format ~template ~@args))))

(def non-nil-entries-xf
  (keep (fn [entry] (when (some? (val entry)) entry))))

(defmacro coalesce-fn
  "Evaluates (f expr) in exprs one at a time, from left to right. If a
  form returns a non-nil, returns that value and doesn't evaluate
  any of the other expressions"
  ([_] nil)
  ([f x & next]
   `(let [x# (~f ~x)]
      (if (some? x#) x# (coalesce-fn ~f ~@next)))))

(defn parse-query-bool ^Boolean [^String line]
  (when line
    (case (str/lower-case line)
      "" nil
      ("true" "on" "1" "yes") true
      ("false" "off" "0" "no") false
      (error! "cannot parse boolean value: %s" line))))

(defn parse-query-long ^Long [^String line]
  (when line
    (case line
      "" nil
      (try
        (Long/parseLong line)
        (catch Exception e
          (error! "cannot parse long value: %s, reason: %s"
                  line (ex-message e)))))))

(defn parse [^String connection-uri]
  (let [connection-uri (str/replace-first connection-uri #"^jdbc:" "")

        uri
        (new URI connection-uri)

        host
        (.getHost uri)

        port
        (.getPort uri)

        port (when (pos? port) port)

        user-info
        (.getUserInfo uri)

        path
        (str (.getPath uri))

        database
        (cond-> path
          (str/starts-with? path "/")
          (subs 1))

        query
        (.getQuery uri)

        query-args
        (when query
          (as-> query *
            (str/split * #"&")
            (mapcat #(str/split % #"=" 2) *)))

        _
        (when (-> query-args count even? not)
          (error! "malformed query params: %s" query))

        query-params
        (apply hash-map query-args)

        [user password]
        (when user-info
          (str/split user-info #":" 2))

        user
        (get query-params "user" (not-empty user))

        password
        (get query-params "password" (not-empty password))

        {:strs [read-only
                options

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

                ;; TODO: unix:// or file:// scheme for unix socket?
                ;; unix-socket
                ;; unix-socket-path

                ;; misc
                cancel-timeout-ms
                protocol-version

                ;; pool
                pool-min-size
                pool-max-size
                pool-expire-threshold-ms
                pool-borrow-conn-timeout-ms

                ;; types
                ;with-pgvector?

                ;; JDBC URI compatibility
                readOnly
                ApplicationName
                cancelSignalTimeout
                binaryTransfer
                tcpKeepAlive
                tcpNoDelay
                protocolVersion]}
        query-params

        config
        {;; common fields
         :host host
         :port port
         :user user
         :password password
         :database database
         :use-ssl? (some? ssl)
         :binary-decode? (coalesce-fn parse-query-bool binary-decode binaryTransfer)
         :binary-encode? (coalesce-fn parse-query-bool binary-encode binaryTransfer)
         :cancel-timeout-ms (coalesce-fn parse-query-long cancel-timeout-ms cancelSignalTimeout)
         :in-stream-buf-size (parse-query-long in-stream-buf-size)
         :out-stream-buf-size (parse-query-long out-stream-buf-size)
         :pool-borrow-conn-timeout-ms (parse-query-long pool-borrow-conn-timeout-ms)
         :pool-expire-threshold-ms (parse-query-long pool-expire-threshold-ms)
         :pool-max-size (parse-query-long pool-max-size)
         :pool-min-size (parse-query-long pool-min-size)
         :protocol-version (coalesce-fn parse-query-long protocol-version protocolVersion)
         :read-only? (coalesce-fn parse-query-bool read-only readOnly)
         :so-keep-alive? (coalesce-fn parse-query-bool so-keep-alive tcpKeepAlive)
         :so-recv-buf-size (parse-query-long so-recv-buf-size)
         :so-send-buf-size (parse-query-long so-send-buf-size)
         :so-tcp-no-delay? (coalesce-fn parse-query-bool so-tcp-no-delay tcpNoDelay)
         :so-timeout (parse-query-long so-timeout)
         :pg-params (cond-> {}
                      options
                      (assoc "options"  options)

                      ApplicationName
                      (assoc "application_name" ApplicationName))}]

    (into {} non-nil-entries-xf config)))
