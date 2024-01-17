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


(let [buf
               (new java.io.ByteArrayOutputStream)

               rows
               (for [x (range 100000)]
                 [x
                  (str "name" x)
                  (LocalDateTime/now)])]

           (with-open [writer (-> buf
                                  io/writer)]
             (csv/write-csv writer rows))

           (pg/copy-in conn
                       "copy aaa (id, name, created_at) from STDIN WITH (FORMAT CSV)"
                       (-> buf (.toByteArray) io/input-stream)))

[{:title "test1",
  :id 4,
  :created_at
  #object[java.time.OffsetDateTime 0x31340eb6 "2024-01-17T21:57:58.660012+03:00"]}
 {:title "test2",
  :id 5,
  :created_at
  #object[java.time.OffsetDateTime 0x11a5aab5 "2024-01-17T21:57:58.660012+03:00"]}
 {:title "test3",
  :id 6,
  :created_at
  #object[java.time.OffsetDateTime 0x3ee200bc "2024-01-17T21:57:58.660012+03:00"]}]


{:title "test2",
 :id 5,
 :created_at
 #object[java.time.OffsetDateTime 0x21782713 "2024-01-17T21:57:58.660012+03:00"]}
