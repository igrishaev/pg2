(ns pg.type
  (:refer-clojure :exclude [vector])
  (:import
   java.util.Map
   java.io.Writer
   (org.pg.type SparseVector
                Point
                Line)
   org.pg.error.PGError
   (org.pg.processor IProcessor
                     Processors)))

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
  [^Point p ^Writer w]
  (.write w (format "<Point %s>" p)))

(defmethod print-method Line
  [^Line l ^Writer w]
  (.write w (format "<Line %s>" l)))


;;
;; Constructors
;;

(defn sparse-vector
  ^SparseVector [^Integer dim ^Map index]
  (let [-index
        (reduce-kv
         (fn [acc k v]
           (assoc acc (int k) (float v)))
         {}
         index)]
    (new SparseVector dim -index)))


(def ^:deprecated ->sparse-vector
  sparse-vector)


(defn sparse-vector? [x]
  (instance? SparseVector x))

;;

(defn point? [x]
  (instance? Point x))


(defn point
  "
  Make an instance of the Point object.
  "
  (^Point [^double x ^double y]
   (new Point x y))

  (^Point [x]
   (cond

     (map? x)
     (Point/fromMap x)

     (vector? x)
     (Point/fromList x)

     (point? x)
     x

     :else
     (throw (PGError/error "wrong point input: %s" x)))))

;;

(defn line? [x]
  (instance? Line x))

(defn line
  "
  Make an instance of the Line object.
  "
  (^Point [^double a ^double b ^double c]
   (new Line a b c))

  (^Point [x]
   (cond

     (map? x)
     (Line/fromMap x)

     (vector? x)
     (Line/fromList x)

     (line? x)
     x

     :else
     (throw (PGError/error "wrong line input: %s" x)))))
