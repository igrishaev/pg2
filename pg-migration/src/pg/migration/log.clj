(ns pg.migration.log)


(defmacro infof [template & args]
  `(println (format ~template ~@args)))
