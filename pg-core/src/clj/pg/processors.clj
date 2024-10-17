(ns pg.processors
  (:refer-clojure :exclude [vector])
  (:import
   org.pg.error.PGError
   org.pg.type.processor.IProcessor
   org.pg.type.processor.Processors))


(def ^IProcessor vector Processors/vector)
(def ^IProcessor bit    Processors/bit)
