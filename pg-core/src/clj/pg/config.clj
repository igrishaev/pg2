(ns pg.config
  "
  A dedicated namespace to build an instance of the Config
  class from a Clojure map. Apparently, this function has
  grown vast so it's better to keep it here.
  "
  (:require
   [pg.common :refer [error!]]
   [pg.connection-uri :as uri])
  (:import
   org.pg.Config
   org.pg.Config$Builder
   org.pg.enums.SSLValidation))


(defn ->SSLValidation
  "
  Coerce a Clojure value to SSLValidation enum.
  "
  ^SSLValidation [x]
  (case x

    (nil false off none no "none" "off" "no" :none :off :no)
    SSLValidation/NONE

    (default :default "default")
    SSLValidation/DEFAULT

    ;; default
    (error! "unknown ssl validation value: %s" x)))


(defn ->config
  "
  Turn a Clojure map into an instance of `Config` via `Config.Builder`.
  First, try to parse the `connection-uri` URI string, if passed.
  Then merge it with other parameters. Finally, build a `Config`
  instance.
  "
  ^Config [params]

  (let [{:keys [connection-uri
                pg-params]}
        params

        uri-params
        (some-> connection-uri uri/parse)

        {uri-pg-params :pg-params}
        uri-params

        pg-params
        (merge uri-pg-params pg-params)

        params
        (merge uri-params params)

        {:keys [;; general
                user
                database
                host
                port
                password

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
                use-ssl? ;; deprecated
                ssl?
                ssl-context
                ssl-validation

                ;; unix domain socket
                unix-socket?
                unix-socket-path

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
                executor

                ;; pool
                pool-min-size
                pool-max-size
                pool-expire-threshold-ms
                pool-borrow-conn-timeout-ms

                ;; types
                type-map]}
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

      (seq pg-params)
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

      (some? ssl?)
      (.useSSL ssl?)

      ssl-validation
      (.sslValidation (->SSLValidation ssl-validation))

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

      (some? unix-socket?)
      (.useUnixSocket unix-socket?)

      unix-socket-path
      (.unixSocketPath unix-socket-path)

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

      executor
      (.executor executor)

      type-map
      (.typeMap type-map)

      :finally
      (.build))))
