(ns pg.client.integration
  (:require
   [clojure.test :refer [testing]]))


(def P11 10110)
(def P12 10120)
(def P13 10130)
(def P14 10140)
(def P15 10150)
(def P16 10160)

(def PORT_SSL
  (some-> "PG_PORT_SSL"
          System/getenv
          Integer/parseInt))


#_
(def OVERRIDES
  {PORT_SSL
   {:ssl? true
    :ssl-context
    (ssl/context {:key-file "../certs/client.key"
                  :cert-file "../certs/client.crt"
                  :ca-cert-file "../certs/root.crt"})}})


(def PORTS
  [P11 P12 P13 P14 P15 P16 PORT_SSL])


(def HOST "127.0.0.1")
(def ^:dynamic *PORT* nil)
(def USER "test")
(def PASS "test")
(def DATABASE "test")


(def ^:dynamic *CONFIG*
  {:host HOST
   :port nil
   :user USER
   :password PASS
   :database DATABASE})


(def ^:dynamic *DB-SPEC*
  {:dbtype "postgres"
   :port nil
   :dbname DATABASE
   :user USER
   :password PASS})


(defn fix-multi-port [t]
  (doseq [port PORTS :when port]
    (binding [*PORT*
              port

              *CONFIG*
              (-> *CONFIG*
                  (assoc :port port)
                  #_
                  (coll/deep-merge (get OVERRIDES port)))

              *DB-SPEC*
              (assoc *DB-SPEC* :port port)]

      (testing (format "PORT %s" port)
        (t)))))


(defn is11? []
  (= *PORT* P11))

(defn is12? []
  (= *PORT* P12))

(defn is13? []
  (= *PORT* P13))

(defn is14? []
  (= *PORT* P14))

(defn is15? []
  (= *PORT* P15))

(defn is16? []
  (= *PORT* P16))

#_
(defn isSSL? []
  (and PORT_SSL (= *PORT* PORT_SSL)))
