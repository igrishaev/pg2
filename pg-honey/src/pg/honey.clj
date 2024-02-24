(ns pg.honey
  "
  HoneySQL wrappers and shortcuts.
  "
  (:refer-clojure :exclude [find
                            update
                            format])
  (:require
   [honey.sql :as sql]
   [pg.core :as pg]))


(def HONEY_OVERRIDES
  {:numbered true})


(defn format
  "
  Like honey.sql/format but with some Postgres-related overrides.
  Namely, it produces a SQL expressions with dollar signs instead
  of question marks, e.g. `SELECT * FROM USERS WHERE id = $1`.
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
  - opt: PG2 options; pass the `:honey` key for HoneySQL params.

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
  - opt: PG2 options; pass the `:honey` key for HoneySQL params.

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


(defn prepare
  "
  Prepare a statement expressed wit a Honey map.
  For parameters, use either blank values or raw
  expressions, for example:

  {:select [:*] :from :users :where [:= :id 0]}

  or

  {:select [:*] :from :users :where [:raw 'id = $1']}

  Return an instance of `PreparedStatement` class.
  "

  ([conn sql-map]
   (prepare conn sql-map nil))

  ([conn sql-map {:as opt :keys [honey]}]
   (let [[sql & params]
         (format sql-map honey)]
     (pg/prepare conn
                 sql
                 (assoc opt :params params)))))


;;
;; Helpers
;;

(defn get-by-id
  "
  Get a single row by its primary key.

  By default, the name of the primary key is `:id`
  which can be overridden by the `:pk` parameter.

  The optimal `:fields` vector specifies which
  columns to return, `[:*]` by default.
  "
  ([conn table id]
   (get-by-id conn table id nil))

  ([conn table id {:as opt
                   :keys [pk
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
  "
  Get multiple rows from a table by their PKs. It's not
  recommended to pass thousands of PKs at once. Split
  them by smaller chunks instead.

  Like `get-by-id`, accepts the PK name, the column names
  to return, and order-by clause.

  Returns a vector of rows.
  "

  ([conn table ids]
   (get-by-ids conn table ids nil))

  ([conn table ids {:as opt
                    :keys [pk
                           fields
                           order-by]
                    :or {pk :id
                         fields [:*]}}]
   (let [sql-map
         (cond-> {:select fields
                  :from table
                  :where [:in pk ids]}

           order-by
           (assoc :order-by order-by))]

     (execute conn sql-map opt))))


(defn insert
  "
  Insert a collection of rows into a table.
  The `kvs` argument must be a seq of maps.

  The optional arguments are:
  - returning: a vector of columns to return, default is [:*]
  - on-conflict,
  - do-update-set,
  - do-nothing: Postgres-specific ON CONFLICT ... DO ... expressions.

  Other PG2-related options are supported.

  The result depends on the `returning` clause and PG2 options.
  By default, it will be a vector of inserted rows.
  "

  ([conn table kvs]
   (insert conn table kvs nil))

  ([conn table kvs {:as opt
                    :keys [returning
                           on-conflict
                           do-update-set
                           do-nothing]
                    :or {returning [:*]}}]
   (let [sql-map
         (cond-> {:insert-into table
                  :values kvs}

           returning
           (assoc :returning returning)

           on-conflict
           (assoc :on-conflict on-conflict)

           do-update-set
           (assoc :do-update-set do-update-set)

           do-nothing
           (assoc :do-nothing do-nothing))]

     (execute conn sql-map opt))))


(defn insert-one
  "
  Like `insert` but accepts a single row.

  Supports the same options. The default result
  is a single inserted row.
  "

  ([conn table kv]
   (insert-one conn table kv nil))

  ([conn table kv {:as opt
                   :keys [returning
                          on-conflict
                          do-update-set
                          do-nothing]
                   :or {returning [:*]}}]
   (let [sql-map
         (cond-> {:insert-into table
                  :values [kv]}

           returning
           (assoc :returning returning)

           returning
           (assoc :returning returning)

           on-conflict
           (assoc :on-conflict on-conflict)

           do-update-set
           (assoc :do-update-set do-update-set)

           do-nothing
           (assoc :do-nothing do-nothing))]

     (execute conn
              sql-map
              (assoc opt :first? true)))))


(defn update
  "
  Update rows in a table. The `kv` is a map of field->value.
  Note that the value might be not a scalar but something
  like `[:raw 'now()']` or any other HoneySQL expression.

  The optional parametes are:
  - `where` which acts like a filter when updating the rows;
  - `returning` which determines what columns to return.
  "
  ([conn table kv]
   (update conn table kv nil))

  ([conn table kv {:as opt
                   :keys [where
                          returning]
                   :or {returning [:*]}}]

   (let [sql-map
         (cond-> {:update table
                  :set kv}

           where
           (assoc :where where)

           returning
           (assoc :returning returning))]

     (execute conn sql-map opt))))


(defn delete
  ([conn table]
   (delete conn table nil))

  ([conn table {:as opt
                :keys [where
                       returning]
                :or {returning [:*]}}]

   (let [sql-map
         (cond-> {:delete-from table}

           where
           (assoc :where where)

           returning
           (assoc :returning returning))]

     (execute conn sql-map opt))))


(defn find
  "
  Select rows by a column->value filter, for example:

  {:name 'Foo', :active true}

  All the kv pairs get concatenated with AND.

  The optional arguments are:
  - fields (default is [:*])
  - limit
  - offset
  - order-by
  "
  ([conn table kv]
   (find conn table kv nil))

  ([conn table kv {:as opt
                   :keys [fields
                          limit
                          offset
                          order-by]
                   :or {fields [:*]}}]

   (let [where
         (when (seq kv)
           (reduce-kv
            (fn [acc k v]
              (conj acc [:= k v]))
            [:and]
            kv))

         sql-map
         (cond-> {:select fields
                  :from table
                  :where where}

           limit
           (assoc :limit limit)

           offset
           (assoc :offset offset)

           order-by
           (assoc :order-by order-by))]

     (execute conn sql-map opt))))


(defn find-first
  "
  Like `find` but gets the first row only. Adds the
  limit 1 expression to the query. Supports the same
  optional arguments.
  "
  ([conn table kv]
   (find-first conn table kv nil))

  ([conn table kv {:as opt
                   :keys [fields
                          offset
                          order-by]
                   :or {fields [:*]}}]

   (let [where
         (when (seq kv)
           (reduce-kv
            (fn [acc k v]
              (conj acc [:= k v]))
            [:and]
            kv))

         sql-map
         (cond-> {:select fields
                  :from table
                  :where where
                  :limit 1}

           offset
           (assoc :offset offset)

           order-by
           (assoc :order-by order-by))]

     (execute conn
              sql-map
              (assoc opt :first? true)))))
