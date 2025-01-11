(ns pg.connection-uri
  (:require
   [clojure.string :as str])
  (:import
   (java.net URI)))

(set! *warn-on-reflection* true)

(defmacro error! [template & args]
  `(throw (new Exception (format ~template ~@args))))

(defn parse-query-bool ^Boolean [^String line]
  (case (str/lower-case line)
    "" nil
    ("true" "on" "1" "yes") true
    ("false" "off" "0" "no") false
    (error! "cannot parse boolean value: %s" line)))

(defn parse-query-long ^Long [^String line]
  (case line
    "" nil
    (try
      (Long/parseLong line)
      (catch Exception e
        (error! "cannot parse long value: %s, reason: %s"
          line (ex-message e))))))

(defn parse [^String connection-uri]
  (let [connection-uri
        (cond-> connection-uri
          (str/starts-with? connection-uri "jdbc:postgresql:")
          (subs 5))

        uri
        (new URI connection-uri)

        host
        (.getHost uri)

        port
        (.getPort uri)

        port
        (cond-> port
          (= -1 port)
          (do nil))

        user-info
        (.getUserInfo uri)

        path
        (.getPath uri)

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

        {:strs [read-only

                ;; socket options
                so-keep-alive
                so-tcp-no-delay
                so-timeout
                so-recv-buf-size
                so-send-buf-size

                ;; bin/text
                binary-encode
                binary-decode

                ;; i
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
                with-pgvector?

                ]}

        query-params

        [user password]
        (when user-info
          (str/split user-info #":" 2))]

    {;; common fields
     :host host
     :port port
     :user user
     :password password
     :database database

     ;; params
     :use-ssl?
     (some-> ssl parse-query-bool)

     :so-keep-alive?
     (some-> so-keep-alive parse-query-bool)

     }


    )



  )
