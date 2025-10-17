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



private boolean _is_used_too_long (final long startTime) {
        return System.currentTimeMillis() - startTime > 123;
    }

    private void _pre_foo_bar () {
        Connection conn;
        for (Map.Entry<UUID, Long> entry : connUsedSince.entrySet()) {
            if (_is_used_too_long(entry.getValue())) {
                conn = connsUsed.get(entry.getKey());
                final String message = String.format(
                        "Connection %s has been considered as leaked, closing",
                        conn.getId()
                );
                logger.log(config.logLevel(), message);
                removeUsed(conn);
                utilizeConnection(conn);
            }
        }
    }


private final Map<UUID, Long> connUsedSince;
this.connUsedSince = new HashMap<>(config.poolMaxSize());
connUsedSince.remove(conn.getId());
connUsedSince.put(conn.getId(), System.currentTimeMillis());

    public static IFn printMethod = Clojure.var("clojure.core", "print-method");

    static {
        final MultiFn mm = (MultiFn) ((Var) CljAPI.printMethod).deref();
        mm.addMethod(RowMap.class, new AFn() {
            @Override
            public Object invoke(final Object valueObj, final Object writerObj) {
                final RowMap value = (RowMap) valueObj;
                final Writer writer = (Writer) writerObj;
                try {
                    writer.write(value.toClojureMap().toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }


    public static IFn require=Clojure.var("clojure.core", "require");
    static {
        require.invoke(clojure.lang.Symbol.intern("clojure.pprint"));
    }
    public static IFn pp=Clojure.var("clojure.pprint", "pprint");



(deftest test-client-custom-type-triple

  (let [typename
        (str "triple_" (System/nanoTime))

        processor
        (reify org.pg.processor.IProcessor
          (decodeBin [this bb codecParams]
            (let [amount
                  (.getInt bb)

                  oid
                  (.getInt bb)
                  len
                  (.getInt bb)
                  a
                  (.getInt bb)

                  oid
                  (.getInt bb)
                  len
                  (.getInt bb)
                  buf
                  (byte-array len)
                  _
                  (.get bb buf)
                  b
                  (new String buf)

                  oid
                  (.getInt bb)
                  len
                  (.getInt bb)
                  c
                  (case (int (.get bb))
                    0 false
                    1 true)]
              {:a a :b b :c c}))

          (decodeTxt [this string codecParams]
            ::fake))]

    (pg/with-connection [conn *CONFIG-BIN*]
      (pg/execute conn (format "create type %s as (a integer, b text, c boolean)" typename)))

    (pg/with-connection [conn (assoc *CONFIG-BIN*
                                     :type-map
                                     {typename processor})]
      (let [res1
            (pg/execute conn (format "select '(1,\"hello,world\",true)'::%s as tuple" typename))]
        (is (= 1
               res1))))))


(doseq [r (range 160 300)]
  (->> oids
       (mapv (fn [oid]
               (mod oid 206)))
       (frequencies)
       (sort-by first)
       (vals)
       (set)
       (println)))
