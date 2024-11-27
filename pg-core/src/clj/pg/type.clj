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
                Polygon
                Path)
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

;; point

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

;; line

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

;; circle

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

;; box

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

;; poly

(defn polygon? [x]
  (instance? Polygon x))

(defn polygon
  "
  Make an instance of the Polygon class.
  "
  (^Polygon [points]
   (Polygon/fromList points)))

;; path

(defn path? [x]
  (instance? Path x))

(defn path
  "
  Make an instance of the Path class.
  "
  (^Path [points closed?]
   (Path/fromList points closed?))

  (^Path [points]
   (Path/fromList points)))
