(ns pg.hugsql
  (:require
   [pg.core :as pg]
   [hugsql.adapter :as adapter]))


(deftype PG2Adapter [defaults]

  adapter/HugsqlAdapter

  (execute [this db sqlvec options]
    (println "execute" options)
    (let [[sql & params]
          sqlvec]
      (pg/execute db sql
                  (assoc defaults :params params))))

  (query [this db sqlvec options]
    (println "query" options)
    (let [[sql]
          sqlvec]
      (pg/execute db sql defaults)))

  (result-one [this result options]
    (println "result-one" options)
    (first result))

  (result-many [this result options]
    (println "result-many" options)
    result)

  (result-affected [this result options]
    (println "result-affected" options)
    (or (:inserted result)
        (:updated result)
        (:deleted result)
        (:selected result)
        (:copied result)))

  (result-raw [this result options]
    (println "result-raw" options)
    result)

  (on-exception [this exception]
    (throw exception)))


(defn adapter
  ([]
   (new PG2Adapter nil))
  ([defaults]
   (new PG2Adapter defaults)))
