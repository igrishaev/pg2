(ns pg.bb
  (:import
   java.nio.ByteBuffer))

(defn bb? [x]
  (instance? ByteBuffer x))

(defn ->bb ^ByteBuffer [coll]
  (ByteBuffer/wrap (byte-array coll)))

(defn bb== [^bytes ba ^ByteBuffer bb]
  (= (vec ba) (vec (.array bb))))
