(ns pg.migration.log
  (:import
   java.lang.System$Logger$Level))


(defmacro infof [template & args]
  `(.log (-> *ns* str System/getLogger)
         System$Logger$Level/INFO
         (format ~template ~@args)))
