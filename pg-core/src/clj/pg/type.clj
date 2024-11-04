(ns pg.type
  (:refer-clojure :exclude [vector])
  (:import
   java.util.Map
   java.io.Writer
   org.pg.type.SparseVector
   org.pg.type.Point
   org.pg.error.PGError
   org.pg.processor.IProcessor
   org.pg.processor.Processors))

;;
;; Processors
;;

(def ^IProcessor vector Processors/vector)

(def ^IProcessor sparsevec Processors/sparsevec)

(def ^IProcessor enum Processors/defaultEnum)


;;
;; Print methods
;;

(defmethod print-method SparseVector
  [^SparseVector sv ^Writer w]
  (.write w (format "<SparseVector %s>" sv)))

(defmethod print-method Point
  [^SparseVector sv ^Writer w]
  (.write w (format "<Point %s>" sv)))


;;
;; Constructors
;;

(defn ->sparse-vector
  ^SparseVector [^Integer dim ^Map index]
  (let [-index
        (reduce-kv
         (fn [acc k v]
           (assoc acc (int k) (float v)))
         {}
         index)]
    (new SparseVector dim -index)))


(defn ->point
  "
  Make an instance of the Point object.
  "
  (^Point [^double x ^double y]
   (new Point x y))

  (^Point [map-or-vec]
   (cond

     (map? map-or-vec)
     (Point/fromMap map-or-vec)

     (vector? map-or-vec)
     (Point/fromList map-or-vec)

     :else
     (throw
      (new PGError
           (format "wrong point input: %s" map-or-vec))))))
