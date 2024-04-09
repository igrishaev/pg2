(ns pg.hugsql
  (:require
   [clojure.string :as str]
   [pg.core :as pg]
   [hugsql.core :as hugsql]
   [hugsql.parameters :as p]
   [hugsql.adapter :as adapter]))


(def ^:dynamic *$* 0)

(defn $next []
  (set! *$* (inc *$*))
  (format "$%d" *$*))


(defn $wrap [f]
  (fn [& args]
    (binding [*$* 0]
      (apply f args))))


(extend-type Object

  p/ValueParam

  (value-param [param data options]
    (let [value
          (get-in data (p/deep-get-vec (:name param)))]
      [($next) value]))

  p/ValueParamList

  (value-param-list [param data options]
    (let [coll
          (get-in data (p/deep-get-vec (:name param)))

          placeholders
          (str/join "," (for [_ coll] ($next)))]

      (into [placeholders] coll))))


(deftype PG2Adapter [defaults]

  adapter/HugsqlAdapter

  (execute [this db sqlvec options]

    (let [{:keys [pg]}
          options

          [sql & params]
          sqlvec

          opt
          (-> defaults
              (merge pg)
              (assoc :params params))]

      (pg/on-connection [conn db]
        (pg/execute db sql opt))))

  (query [this db sqlvec options]
    (adapter/execute this db sqlvec options))

  (result-one [this result options]
    (first result))

  (result-many [this result options]
    result)

  (result-affected [this result options]
    (or (:inserted result)
        (:updated result)
        (:deleted result)
        (:selected result)
        (:copied result)))

  (result-raw [this result options]
    result)

  (on-exception [this exception]
    (throw exception)))


(defn make-adapter
  ([]
   (new PG2Adapter nil))
  ([defaults]
   (new PG2Adapter defaults)))


(defn intern-function [fn-name
                       fn-meta
                       fn-obj]
  (let [sym
        (-> fn-name name symbol)]
    (intern *ns*
            (with-meta sym fn-meta)
            ($wrap fn-obj))))


(defn def-db-fns
  ([file]
   (def-db-fns file nil nil))

  ([file options]
   (def-db-fns file options nil))

  ([file options defaults]

   (let [adapter
         (make-adapter defaults)

         defs
         (hugsql/map-of-db-fns
          file
          (assoc options :adapter adapter))]

     (doseq [[fn-name {fn-meta :meta
                       fn-obj :fn}]
             defs]
       (intern-function fn-name fn-meta fn-obj)))))
