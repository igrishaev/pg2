(ns pg.migration.log
  "
  Logging facilities.
  ")


(defmacro infof [template & args]
  `(println (format ~template ~@args)))
