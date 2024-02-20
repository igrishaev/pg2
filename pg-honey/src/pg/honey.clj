(ns pg.honey
  (:refer-clojure :exclude [update
                            format])
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


;;
;; Helpers
;;

(defn get-by-id
  ([conn table id]
   (get-by-id conn table id nil))

  ([conn table id {:as opt
                   :keys [id
                          fields]
                   :or {pk :id
                        fields [:*]}}]
   (let [sql-map
         {:select fields
          :from table
          :where [:= pk id]
          :limit 1}]

     (execute conn
              sql-map
              (assoc opt :first? true)))))


(defn get-by-ids
  ([conn table ids]
   (get-by-ids conn table ids nil))

  ([conn table ids {:as opt
                    :keys [id
                           fields]
                    :or {pk :id
                         fields [:*]}}]
   (let [sql-map
         {:select fields
          :from table
          :where [:in pk id]}]

     (execute conn sql-map opt))))


(defn insert

  ([conn table maps]
   (insert conn table maps nil))

  ([conn table maps {:as opt
                     :keys [returning]
                     :or {returning [:*]}}]
   (let [sql-map
         {:insert-into table
          :values maps
          :returning returning}]

     (execute conn sql-map opt))))


(defn update
  ([conn table set]
   (update conn table set nil))

  ([conn table set {:as opt
                    :keys [where
                           returning]
                    :or {where nil
                         returning [:*]}}]

   (let [sql-map
         {:udpate table
          :set set
          :where where
          :returning returning}]

     (execute conn sql-map opt))))


(defn delete
  ([conn table]
   (delete conn table nil))

  ([conn table {:as opt
                :keys [where
                       returning]
                :or {where nil
                     returning [:*]}}]

   (let [sql-map
         {:delete table
          :where where
          :returning returning}]

     (execute conn sql-map opt))))
