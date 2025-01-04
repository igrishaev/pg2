(ns pg.migration.err)

(defmacro throw! [template & args]
  `(throw (new RuntimeException (format ~template ~@args))))

(defmacro rethrow! [e template & args]
  `(throw (new RuntimeException
               (format ~template ~@args)
               ~e)))
