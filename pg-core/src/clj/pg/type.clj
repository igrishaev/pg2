(ns pg.type
  (:refer-clojure :exclude [vector])
  (:import
   java.util.Map
   java.io.Writer
   (org.pg.type SparseVector
                Point
                Line
                Circle
                Box)
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

(defmethod print-method Circle
  [^Circle c ^Writer w]
  (.write w (format "<Circle %s>" c)))


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
   (Point/of x y))

  (^Point [x]
   (Point/fromObject x)))

;;

(defn line? [x]
  (instance? Line x))

(defn line
  "
  Make an instance of the Line object.
  "
  (^Line [^double a ^double b ^double c]
   (Line/of a b c))

  (^Line [x]
   (Line/fromObject x)))

;;

(defn circle? [x]
  (instance? Circle x))

(defn circle?
  "
  Make an instance of the Circle object.
  "
  (^Circle [x y r]
   (Circle/of x y r))

  (^Circle [x]
   (Circle/fromObject x)))
