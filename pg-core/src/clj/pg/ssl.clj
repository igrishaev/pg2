(ns pg.ssl
  "
  SSL utilities, mostly to build instances
  of the `SSLContext` class.
  "
  (:require
   [less.awful.ssl :as ssl])
  (:import
   (javax.net.ssl SSLContext
                  TrustManager)))


(defn context
  "
  Build an instance of `SSLContext` class from various
  combinations of key, cert, and CA files.
  "
  ;; build from a single CA .cert file
  (^SSLContext [^String ca-cert-file]
   (let [trust-manager (-> ca-cert-file
                           (ssl/trust-store)
                           (ssl/trust-manager))]
     (doto (SSLContext/getInstance "TLSv1.2")
       (.init nil
              (into-array TrustManager [trust-manager])
              nil))))

  ;; from a key file + cert file
  (^SSLContext [^String key-file
                ^String cert-file]
   (ssl/ssl-context key-file cert-file))

  ;; all three
  (^SSLContext [^String key-file
                ^String cert-file
                ^String ca-cert-file]
   (ssl/ssl-context key-file cert-file ca-cert-file)))


(defn context-reader [files]
  `(context ~@files))
