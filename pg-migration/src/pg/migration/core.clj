(ns pg.migration.core
  "
  Common migration functions.
  "
  (:import
   clojure.lang.Keyword
   clojure.lang.Sorted
   java.net.URL
   java.net.URLDecoder
   java.time.OffsetDateTime
   java.time.ZoneOffset
   java.time.format.DateTimeFormatter
   org.pg.Connection)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [pg.core :as pg]
   [pg.honey :as pgh]
   [pg.migration.fs :as fs]
   [pg.migration.log :as log]))

(set! *warn-on-reflection* true)

(def DEFAULTS
  {:migrations-table :migrations
   :migrations-path "migrations"})


(def RE_FILE
  #"(?i)(\d+)\.(.*?)\.?(prev|next|up|down)\.sql$")


(defn parse-name [^String file-name]
  (re-matches RE_FILE file-name))


(def ^DateTimeFormatter DATETIME_PATTERN
  (-> "yyyyMMddHHmmss"
      (DateTimeFormatter/ofPattern)
      (.withZone ZoneOffset/UTC)))


(defmacro error! [template & args]
  `(throw (new Error (format ~template ~@args))))


(defn letters
  "
  Repeat a certain string/character N times.
  "
  ([n]
   (letters n \space))
  ([n ch]
   (str/join (repeat n ch))))


(defn generate-datetime-id
  "
  Generate a Long number based on the current UTC datetime
  of a format YYYYmmddHHMMSS, e.g. 20240308154432.
  "
  ^Long []
  (-> (java.time.OffsetDateTime/now)
      (.format DATETIME_PATTERN)
      (Long/parseLong)))


(defn text->slug
  "
  Turn a 'Human  Friendly Text ' into something like
  'human-friendly-text' to be a part of a migration file.
  "
  ^String [^String text]
  (-> text
      (str/lower-case)
      (str/trim)
      (str/replace #"\s+" "-")
      (str/replace #"[^a-zA-Z0-9-_]" "")))


(defn make-file-name
  "
  Compose a file name based on the id,
  an optional slug, and the direction type.
  "
  ^String [id text direction]
  (let [slug (some-> text text->slug)]
    (with-out-str
      (print id)
      (when-not (str/blank? slug)
        (print ".")
        (print slug))
      (print ".")
      (print
       (case direction
         :prev "prev"
         :next "next"))
      (print ".sql"))))


(defn create-migration-files
  "
  Create a couple of new migration files, prev and next.
  When not set, the id is generated from the current UTC time.
  Return a pair of `File` objects, prev and next.
  "

  ([url]
   (create-migration-files url nil))

  ([url {:keys [id slug]}]
   (let [id
         (or id
             (generate-datetime-id))

         dir
         (fs/url->dir url)

         name-prev
         (make-file-name id slug :prev)

         name-next
         (make-file-name id slug :next)

         file-prev
         (io/file dir name-prev)

         file-next
         (io/file dir name-next)]

     (.mkdirs (io/file dir))

     (spit file-prev "")
     (spit file-next "")

     [file-prev file-next])))


(defn cleanup-slug
  "
  Turn a slug fragment into a human-friendly text.
  "
  ^String [^String slug]
  (-> slug
      (str/replace #"-|_" " ")
      (str/replace #"\s+" " ")
      (str/trim)))


(defn ensure-table
  "
  Having a db connection and the name of the migrations table,
  create a table if it doesn't exist.
  "
  [^Connection conn ^Keyword migrations-table]
  (let [query
        {:create-table [migrations-table :if-not-exists]
         :with-columns
         [[:id :bigint :primary-key]
          [:slug :text]
          [:created_at [:raw "timestamp with time zone not null default current_timestamp"]]]}]
    (pgh/query conn query)))


(defn parse-url
  "
  Split a URL pointing to a migration file to various fields.
  "
  [^URL url]
  (when-let [[_ id-raw slug-raw direction-raw]
             (re-matches RE_FILE (-> url
                                     .getFile
                                     URLDecoder/decode
                                     io/file
                                     .getName))]

    (let [id
          (Long/parseLong id-raw)

          slug
          (cleanup-slug slug-raw)

          direction-keyword
          (-> direction-raw
              str/lower-case
              keyword)

          direction
          (case direction-keyword
            (:prev :down) :prev
            (:next :up) :next)]

      {:id id
       :slug slug
       :direction direction
       :url url})))


(defn group-parsed-urls
  "
  Having a seq of maps (parsed URLs), unify them by id.
  Return a sorted map, an instance of `clojure.lang.Sorted`.
  "
  ^Sorted [parsed-urls]
  (reduce
   (fn [acc {:keys [id slug direction url]}]
     (let [node
           (cond-> {:id id
                    :slug slug}
             (= direction :prev)
             (assoc :url-prev url)
             (= direction :next)
             (assoc :url-next url))]
       (update acc id merge node)))
   (sorted-map)
   parsed-urls))


(defn validate-duplicates!
  "
  Having a seq of parsed URLs, find those that have the same
  (id, direction) pair. If found, an exception is thrown.
  "
  [parsed-urls]
  (let [f
        (juxt :id :direction)

        result
        (group-by f parsed-urls)]

    (doseq [[[id direction] items] result]
      (when (-> items count (> 1))
        (let [urls
              (for [item items]
                (-> item ^URL (:url) (.getFile)))]

          (error! "Migration %s has %s %s files: %s"
                  id
                  (count items)
                  (name direction)
                  (str/join ", " urls))))))

  parsed-urls)


(defn url->migrations
  "
  Fetch all the migrations from the top-level URL.
  Return a sorted map like (id => migration-map).
  "
  ^Sorted [^URL url]
  (->> url
       (fs/url->children)
       (map parse-url)
       (filter some?)
       (validate-duplicates!)
       (group-parsed-urls)))


(defn read-disk-migrations
  "
  Read all the migrations from a resource.
  "
  ^Sorted [^URL url]
  (url->migrations url))


(defn get-applied-migration-ids
  "
  Read all the applied migrations from the database
  and return a set of their ids.
  "
  [^Connection conn ^Keyword table]
  (let [query
        {:select [:id]
         :from table
         :order-by [[:id :asc]]}]
    (->> query
         (pgh/query conn)
         (map :id)
         (set)
         (not-empty))))


(defn validate-conflicts!
  "
  Try to find a situation when a migration with less ID
  was applied before another migration with a greater ID.

  For example:

  id applied
   1 yes
   2 yes
   3 no
   4 yes

  Above, 3 is less then 4 but 4 has been applied whereas 3 has not.
  Usually it happens when two features get merged at the same time.
  To fix it, rename the migration 3 to 5 as follows:

  id applied
   1 yes
   2 yes
   4 yes
   5 no

  "
  [^Sorted migrations]
  (doseq [i (range 0 (-> migrations count dec))]
    (let [migration1 (get migrations i)
          migration2 (get migrations (inc i))]
      (when (and (-> migration1 :applied? not)
                 (-> migration2 :applied?))
        (error! "Migration conflict: migration %s has been applied before %s"
                (:id migration2)
                (:id migration1))))))


(defn validate-migration-id!
  "
  Check if the migration map has a certain id.
  "
  [^Sorted migrations id]
  (when-not (contains? migrations id)
    (error! "Migration %s doesn't exist" id)))


(defn make-scope
  "
  Having a raw config map, do the following:
  - apply config defautls
  - build migrations map
  - get applied migration IDs from the database
  - mark applied migrations;
  - detect the current migration;

  Return a map of all these fields.
  "
  [config]

  (let [config
        (merge DEFAULTS config)

        {:keys [migrations-table
                migrations-path]}
        config]

    (pg/with-connection [conn config]

      (ensure-table conn migrations-table)

      (let [applied-ids-set
            (get-applied-migration-ids conn migrations-table)

            migrations
            (read-disk-migrations migrations-path)

            id-current
            (apply max -1 applied-ids-set)

            migrations
            (reduce-kv
             (fn [acc id migration]
               (let [flag (contains? applied-ids-set id)]
                 (assoc-in acc [id :applied?] flag)))
             migrations
             migrations)]

        (validate-conflicts! (vals migrations))

        {:config config
         :id-current id-current
         :migrations migrations
         :migrations-table migrations-table
         :migrations-path migrations-path}))))


(defn log-connection-info
  "
  Report the connection information.
  "
  [config]
  (let [{:keys [user
                password
                host
                port
                database]}
        config

        len
        (count password)

        stars
        (letters len \*)]

    (log/infof "Connection: %s:%s@%s:%s/%s"
      user
      stars
      host
      port
      database)))


(defn log-sql [^URL url sql]
  (if url
    (do
      (log/infof "File: %s" (.getFile url))
      (log/infof sql))
    (log/infof "The file is missing")))


(defn -migrate
  "
  A general migrate forward function. Don't use it directly.
  Accepts the scope map and a seq of pairs (id, migration).
  Runs the migrations one by one. Tracks the applied migrations
  in the database.
  "
  [scope id-migration's]

  (let [{:keys [config
                migrations-table]}
        scope]

    (log-connection-info config)

    (log/infof "Running next migrations: %s"
      (->> id-migration's keys (str/join ", ") ))

    (pg/with-connection [conn config]

      (doseq [[id migration]
              id-migration's]

        (let [title
              (format "Processing next migration %s" id)

              dash
              (letters (count title) \-)

              {:keys [slug url-next]}
              migration

              sql
              (some-> url-next slurp)]

          (log/infof dash)
          (log/infof title)
          (log/infof dash)

          (log-sql url-next sql)

          (when sql
            (pg/query conn sql))
          (pgh/insert-one conn
                          migrations-table
                          {:id id :slug slug}))))))


(defn migrate-to
  "
  Migrate forward to a certain migration by its ID.
  "
  [config id-to]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        _
        (validate-migration-id! migrations id-to)

        pending-migrations
        (subseq migrations > id-current <= id-to)]

    (-migrate scope pending-migrations)))


(defn migrate-all
  "
  Migrate all the pending forward migrations.
  "
  [config]
  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (subseq migrations > id-current)]

    (-migrate scope pending-migrations)))


(defn migrate-one
  "
  Apply the next single migration.
  "
  [config]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (take 1 (subseq migrations > id-current))]

    (-migrate scope pending-migrations)))


(defn -rollback
  "
  A general rollback function. Don't use it directly.
  Accepts the scope map and a seq of pairs (id, migration).
  Rolls back the migrations one by one. Removes the records
  from the database.
  "
  [scope id-migration's]

  (let [{:keys [config
                migrations-table]}
        scope]

    (log-connection-info config)

    (log/infof "Running previous migrations: %s"
      (->> id-migration's keys (str/join ", ") ))

    (pg/with-connection [conn config]

      (doseq [[id migration]
              id-migration's]

        (let [{:keys [slug
                      url-prev]}
              migration

              title
              (format "Processing prev migration %s" id)

              dash
              (letters (count title) \-)

              sql
              (some-> url-prev slurp)]

          (log/infof dash)
          (log/infof title)
          (log/infof dash)

          (log-sql url-prev sql)

          (when sql
            (pg/query conn sql))
          (pgh/delete conn
                      migrations-table
                      {:where [:= :id id]}))))))


(defn rollback-to
  "
  Rollback to a certain migration by its ID.
  "
  [config id-to]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        _
        (validate-migration-id! migrations id-to)

        pending-migrations
        (rsubseq migrations > id-to <= id-current)]

    (-rollback scope pending-migrations)))


(defn rollback-all
  "
  Rollback all the previous migrations.
  "
  [config]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (rsubseq migrations <= id-current)]

    (-rollback scope pending-migrations)))


(defn rollback-one
  "
  Rollback the current migration.
  "
  [config]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (take 1 (rsubseq migrations <= id-current))]

    (-rollback scope pending-migrations)))
