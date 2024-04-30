(ns pg.honey
  "
  HoneySQL wrappers and shortcuts.
  "
  (:refer-clojure :exclude [find
                            update
                            format])
  (:require
   [clojure.string :as str]
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
  - src: a connectable source;
  - sql-map: a map like {:select [:this :that] :from [...]}
  - opt: PG2 options; pass the `:honey` key for HoneySQL params.

  Result:
  - the same as `pg.core/query`.
  "

  ([src sql-map]
   (query src sql-map nil))

  ([src sql-map {:as opt :keys [honey]}]

   (let [[sql]
         (format sql-map honey)]
     (pg/on-connection [conn src]
       (pg/query conn sql opt)))))


(defn queries
  "
  Like `query` but accepts a vector of SQL maps.
  Returns a vector of results.
  "
  ([src sql-maps]
   (queries src sql-maps nil))

  ([src sql-maps {:as opt :keys [honey]}]

   (let [sql-vecs
         (for [sql-map sql-maps]
           (format sql-map honey))

         sql
         (->> sql-vecs
              (map first)
              (str/join "; "))]

     (pg/on-connection [conn src]
       (pg/query conn sql opt)))))


(defn execute
  "
  Like `pg.core/execute` but accepts a HoneySQL map
  which gets rendered into SQL vector and split on a query
  and parameters.

  Arguments:
  - conn: a connectable source;
  - sql-map: a map like {:select [:this :that] :from [...]}
  - opt: PG2 options; pass the `:honey` key for HoneySQL params.

  Result:
  - same as `pg.core/execute`.
  "

  ([src sql-map]
   (execute src sql-map nil))

  ([src sql-map {:as opt :keys [honey]}]
   (let [[sql & params]
         (format sql-map honey)]
     (pg/on-connection [conn src]
       (pg/execute conn
                   sql
                   (assoc opt :params params))))))


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

  ([src sql-map]
   (prepare src sql-map nil))

  ([src sql-map {:as opt :keys [honey]}]
   (let [[sql & params]
         (format sql-map honey)]
     (pg/on-connection [conn src]
       (pg/prepare conn
                   sql
                   (assoc opt :params params))))))


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
  ([src table id]
   (get-by-id src table id nil))

  ([src table id {:as opt
                  :keys [pk
                         fields]
                  :or {pk :id
                       fields [:*]}}]
   (let [sql-map
         {:select fields
          :from table
          :where [:= pk id]
          :limit 1}]

     (execute src
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

  ([src table ids]
   (get-by-ids src table ids nil))

  ([src table ids {:as opt
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

     (execute src sql-map opt))))


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

  ([src table kvs]
   (insert src table kvs nil))

  ([src table kvs {:as opt
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

     (execute src sql-map opt))))


(defn insert-one
  "
  Like `insert` but accepts a single row.

  Supports the same options. The default result
  is a single inserted row.
  "

  ([src table kv]
   (insert-one src table kv nil))

  ([src table kv {:as opt
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

     (execute src
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
  ([src table kv]
   (update src table kv nil))

  ([src table kv {:as opt
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

     (execute src sql-map opt))))


(defn delete
  ([src table]
   (delete src table nil))

  ([src table {:as opt
               :keys [where
                      returning]
               :or {returning [:*]}}]

   (let [sql-map
         (cond-> {:delete-from table}

           where
           (assoc :where where)

           returning
           (assoc :returning returning))]

     (execute src sql-map opt))))


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
  ([src table]
   (find src table nil nil))

  ([src table kv]
   (find src table kv nil))

  ([src table kv {:as opt
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
                  :from table}

           where
           (assoc :where where)

           limit
           (assoc :limit limit)

           offset
           (assoc :offset offset)

           order-by
           (assoc :order-by order-by))]

     (execute src sql-map opt))))


(defn find-first
  "
  Like `find` but gets the first row only. Adds the
  limit 1 expression to the query. Supports the same
  optional arguments.
  "
  ([src table kv]
   (find-first src table kv nil))

  ([src table kv {:as opt
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

     (execute src
              sql-map
              (assoc opt :first? true)))))
