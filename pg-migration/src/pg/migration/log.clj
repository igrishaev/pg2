(ns pg.migration.log
  "
  Logging facilities.
  ")


(defmacro infof [template & args]
  `(println (format ~template ~@args)))

(defmacro info [& args]
  `(println ~@args))
