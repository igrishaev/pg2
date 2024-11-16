(ns pg.ssl
  "
  SSL utilities, mostly to build instances
  of the `SSLContext` class.
  "
  (:require
   [less.awful.ssl :as ssl])
  (:import
   (javax.net.ssl SSLContext)))


(defn context
  "
  Build an instance of `SSLContext` class from various
  combinations of key, cert, and CA files.
  "
  ;; build from a single CA .cert file
  (^SSLContext [^String ca-cert-file]
   (ssl/ssl-context ca-cert-file))

  ;; from a key file + cert file
  (^SSLContext [^String key-file
                ^String cert-file]
   (ssl/ssl-context key-file cert-file))

  ;; all the three
  (^SSLContext [^String key-file
                ^String cert-file
                ^String ca-cert-file]
   (ssl/ssl-context key-file cert-file ca-cert-file)))


(defn context-reader [files]
  `(context ~@files))
