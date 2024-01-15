(ns pg.honey
  (:refer-clojure :exclude [format])
  (:require
   [honey.sql :as sql]
   [pg.core :as pg]))


(def HONEY_OVERRIDES
  {:numbered true})


(defn format
  "
  Like honey.sql/format but with some overrides.
  "

  ([sql-map]
   (format sql-map nil))

  ([sql-map opt]
   (sql/format sql-map (merge opt HONEY_OVERRIDES))))


(defn query
  "
  Like `pg.core/query` but accepts a HoneySQL map
  which gets rendered into a SQL string.

  Arguments:
  - conn: a Connection object;
  - sql-map: a map like {:select [:this :that] :from [...]}
  - opt: query options; pass the `:honey` key for HoneySQL params.

  Result:
  - the same as `pg.core/query`.
  "

  ([conn sql-map]
   (query conn sql-map nil))

  ([conn sql-map {:as opt :keys [honey]}]

   (let [[sql]
         (format sql-map honey)]
     (pg/query conn sql opt))))


(defn execute
  "
  Like `pg.core/execute` but accepts a HoneySQL map
  which gets rendered into SQL vector and split on a query
  and parameters.

  Arguments:
  - conn: a Connection object;
  - sql-map: a map like {:select [:this :that] :from [...]}
  - opt: query options; pass the `:honey` key for HoneySQL params.

  Result:
  - same as `pg.core/execute`.
  "

  ([conn sql-map]
   (execute conn sql-map nil))

  ([conn sql-map {:as opt :keys [honey]}]
   (let [[sql & params]
         (format sql-map honey)]
     (pg/execute conn
                 sql
                 (assoc opt :params params)))))
