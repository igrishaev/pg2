;; TODO: SSL tests

#_
(def ssl-context
  (ssl/ssl-context "../certs/client.key"
                   "../certs/client.crt"
                   "../certs/root.crt"))


#_
(def ^:dynamic *CONFIG*
  {:host "127.0.0.1"
   :port 10130
   ;; :port 15432 ;; ssl
   :user "test"
   :password "test"
   :database "test"

   ;; :use-ssl? true
   ;; :ssl-context ssl-context
   })


(def PORT_SSL
  (some-> "PG_PORT_SSL"
          System/getenv
          Integer/parseInt))


(def OVERRIDES
  {PORT_SSL
   {:ssl? true
    :ssl-context
    (ssl/context {:key-file "../certs/client.key"
                  :cert-file "../certs/client.crt"
                  :ca-cert-file "../certs/root.crt"})}})


(defn isSSL? []
  (and PORT_SSL (= *PORT* PORT_SSL)))
