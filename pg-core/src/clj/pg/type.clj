(ns pg.type
  (:refer-clojure :exclude [vector])
  (:import
   java.io.Writer
   org.pg.type.SparseVector
   org.pg.error.PGError
   org.pg.type.processor.IProcessor
   org.pg.type.processor.Processors))


(def ^IProcessor vector Processors/vector)

(def ^IProcessor sparsevec Processors/sparsevec)

(defmethod print-method SparseVector
  [^SparseVector sv ^Writer w]
  (.write w (format "<SparseVector %s>" sv)))
