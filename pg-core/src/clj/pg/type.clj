(ns pg.type
  (:refer-clojure :exclude [vector])
  (:import
   java.util.Map
   java.io.Writer
   (org.pg.type SparseVector
                Point
                Line
                Circle
                Box
                Polygon)
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
  [p ^Writer w]
  (print-method @p w))

;; (prefer-method print-method Point clojure.lang.IPersistentMap)

;; (prefer-method print-method clojure.lang.IPersistentMap clojure.lang.IDeref)

(prefer-method print-method Point clojure.lang.IDeref)

(require '[clojure.pprint :as pprint] )

#_
(prefer-method pprint/simple-dispatch
               ;; Point
               clojure.lang.IPersistentMap
               clojure.lang.IDeref
               )

#_
(defmethod pprint/simple-dispatch Point
  [point]
  (pprint/pprint-map point))

(defmethod pprint/simple-dispatch Point
  [point]
  (pprint/simple-dispatch @point))

;; error: java.lang.IllegalArgumentException: Multiple methods in multimethod 'simple-dispatch' match dispatch value: class org.pg.type.Point -> interface clojure.lang.IPersistentMap and interface clojure.lang.IDeref, and neither is preferred

;;    error: java.lang.IllegalArgumentException: Multiple methods in multimethod 'simple-dispatch' match dispatch value:
;; class org.pg.type.Point -> interface clojure.lang.IPersistentMap and interface clojure.lang.IDeref, and neither is preferred

(defmethod print-method Line
  [^Line l ^Writer w]
  (.write w (format "<Line %s>" l)))

(defmethod print-method Circle
  [^Circle c ^Writer w]
  (.write w (format "<Circle %s>" c)))

(defmethod print-method Box
  [^Box b ^Writer w]
  (.write w (format "<Box %s>" b)))

(defmethod print-method Polygon
  [^Polygon p ^Writer w]
  (.write w (format "<Polygon %s>" p)))


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
  Make an instance of the Point class.
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
  Make an instance of the Line class.
  "
  (^Line [^double a ^double b ^double c]
   (Line/of a b c))

  (^Line [x]
   (Line/fromObject x)))

;;

(defn circle? [x]
  (instance? Circle x))

(defn circle
  "
  Make an instance of the Circle class.
  "
  (^Circle [x y r]
   (Circle/of x y r))

  (^Circle [x]
   (Circle/fromObject x)))

;;

(defn box? [x]
  (instance? Box x))

(defn box
  "
  Make an instance of the Box class.
  "
  (^Box [x1 y1 x2 y2]
   (Box/of x1 y1 x2 y2))

  (^Box [p1 p2]
   (Box/of (point p1) (point p2)))

  (^Box [x]
   (Box/fromObject x)))

;;

(defn polygon? [x]
  (instance? Polygon x))

(defn polygon
  (^Polygon [points]
   (Polygon/fromList points)))
