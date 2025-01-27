(ns pg.common
  "
  Things needed across many namespaces.
  "
  (:import
   org.pg.error.PGError))

(set! *warn-on-reflection* true)


(defmacro error!
  ([message]
   `(throw (new PGError ~message)))

  ([template & args]
   `(throw (new PGError (format ~template ~@args)))))
