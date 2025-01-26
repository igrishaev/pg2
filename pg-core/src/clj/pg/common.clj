(ns pg.common
  (:import
   org.pg.error.PGError))

(set! *warn-on-reflection* true)


(defn error!
  ([message]
   (throw (new PGError message)))

  ([template & args]
   (throw (new PGError (apply format template args)))))
