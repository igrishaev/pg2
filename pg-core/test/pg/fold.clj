(ns pg.fold
  (:import
   java.io.Writer
   java.util.ArrayList
   java.util.List
   org.pg.clojure.LazyMap)
  (:require
   [pg.json :as json])
  (:refer-clojure :exclude [first
                            map
                            group-by
                            into
                            reduce]))


(defn column [col]
  (fn folder-column
    ([]
     (transient []))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (conj! acc! (get row col)))))


(defn map [f]
  (fn folder-map
    ([]
     (transient []))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (conj! acc! (f row)))))


(defn default
  ([]
   (transient []))
  ([acc!]
   (persistent! acc!))
  ([acc! row]
   (conj! acc! row)))


(defn dummy
  ([] nil)
  ([acc] nil)
  ([acc row] nil))


(defn first
  ([] nil)
  ([acc] acc)
  ([acc row]
   (if (nil? acc)
     row
     acc)))


(defn reduce [f init]
  (fn folder-reduce
    ([] init)
    ([acc] acc)
    ([acc row]
     (f acc row))))


(defn index-by [f]
  (fn folder-index-by
    ([]
     (transient {}))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (assoc! acc! (f row) row))))


(defn group-by [f]
  (let [-conj (fnil conj [])]
    (fn folder-group-by
      ([] {})
      ([acc] acc)
      ([acc row]
       (update acc (f row) -conj row)))))


(def index-by-id
  (index-by :id))


(defn java
  ([] (new ArrayList))
  ([acc] acc)
  ([^List acc row]
   (doto acc
     (.add row))))


(defn kv [fk fv]
  (fn folder-kv
    ([]
     (transient []))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (assoc! acc! (fk row) (fv row)))))


(defn run [f]
  (fn folder-run
    ([] 0)
    ([acc] acc)
    ([acc row]
     (f row)
     (inc acc))))


(defn into
  ([xform]
   (into xform []))
  ([xform to]
   (let [f (xform conj!)]
     (fn folder-into
       ([]
        (transient to))
       ([acc!]
        (persistent! acc!))
       ([acc! row]
        (f acc! row))))))


(defn to-edn [^Writer writer]
  (fn folder-to-edn
    ([]
     (.write writer "[\n")
     0)

    ([acc]
     (.write writer "]\n")
     acc)

    ([acc row]
     (.write writer "  ")
     (.write writer (pr-str row))
     (.write writer "\n")
     (inc acc))))


(defn to-json [^Writer writer]
  (let [-sent? (volatile! false)]
    (fn folder-to-json
      ([]
       (.write writer "[\n")
       0)

      ([acc]
       (.write writer "\n]\n")
       acc)

      ([acc row]
       (println @-sent?)
       (if @-sent?
         (do
           (.write writer ",\n  ")
           (.write writer (json/write-string row)))
         (do
           (.write writer "  ")
           (.write writer (json/write-string row))
           (vreset! -sent? true)))
       (inc acc)))))


(defn matrix
  ([]
   (matrix false))

  ([header?]
   (if header?
     (let [-set? (volatile! false)]
       (fn folder-matrix
         ([]
          (transient []))
         ([acc!]
          (persistent! acc!))
         ([acc! ^LazyMap row]
          (if @-set?
            (conj! acc! (.toLazyVector row))
            (do
              (vreset! -set? true)
              (-> acc!
                  (conj! (.keys row))
                  (conj! (.toLazyVector row))))))))

     (fn folder-matrix
       ([]
        (transient []))
       ([acc!]
        (persistent! acc!))
       ([acc! ^LazyMap row]
        (conj! acc! (.toLazyVector row)))))))
