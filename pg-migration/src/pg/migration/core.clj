(ns pg.migration.core
  (:import
   java.net.URL
   java.net.URLDecoder
   java.time.OffsetDateTime
   java.time.ZoneOffset
   java.time.format.DateTimeFormatter)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [pg.core :as pg]
   [pg.honey :as pgh]
   [pg.migration.log :as log]
   [pg.migration.fs :as fs]))


;; TODO
;; - common --help?
;; - common args as vars
;; - refactor logs?
;; - split config and options?
;; - fix exit
;; - cli tests
;; - throw in unknown migration
;; - docstrings
;; - demo
;; - docs
;; - lein plugin

(def DEFAULTS
  {:migrations-table :migrations
   :migrations-path "migrations"})


(def RE_FILE
  #"(?i)^.*?/(\d+)(.*?)\.(prev|next)\.sql$")


(def ^DateTimeFormatter DATETIME_PATTERN
  (-> "yyyyMMddHHmmss"
      (DateTimeFormatter/ofPattern)
      (.withZone ZoneOffset/UTC)))


(defn generate-datetime-id ^Long []
  (-> (java.time.OffsetDateTime/now)
      (.format DATETIME_PATTERN)
      (Long/parseLong)))


(defn text->slug ^String [^String text]
  (-> text
      (str/lower-case)
      (str/trim)
      (str/replace #"\s+" " ")
      (str/replace #"\s" "-")))


(defn make-file-name [id text direction]
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
  ([migrations-path]
   (create-migration-files migrations-path nil))

  ([migrations-path {:keys [id slug]}]
   (let [id
         (or id
             (generate-datetime-id))

         name-prev
         (make-file-name id slug :prev)

         name-next
         (make-file-name id slug :next)

         file-prev
         (io/file migrations-path name-prev)

         file-next
         (io/file migrations-path name-next)]

     (.mkdirs (io/file migrations-path))

     (spit file-prev "")
     (spit file-next "")

     [file-prev file-next])))


(defn cleanup-slug ^String [^String slug]
  (-> slug
      (str/replace #"-|_" " ")
      (str/replace #"\s+" " ")
      (str/trim)))


(defn ensure-table [conn migrations-table]
  (let [query
        {:create-table [migrations-table :if-not-exists]
         :with-columns
         [[:id :bigint :primary-key]
          [:slug :text]
          [:created_at [:raw "timestamp with time zone not null default current_timestamp"]]]}]
    (pgh/query conn query)))


(defn parse-url [^URL url]
  (when-let [[_ id-raw slug-raw direction-raw]
             (re-matches RE_FILE (-> url
                                     .getFile
                                     URLDecoder/decode))]

    (let [id
          (Long/parseLong id-raw)

          slug
          (cleanup-slug slug-raw)

          direction
          (-> direction-raw
              str/lower-case
              keyword)]

      {:id id
       :slug slug
       :direction direction
       :url url})))


(defn group-parsed-urls [parsed-urls]
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


(defn validate-duplicates! [parsed-urls]
  (let [f
        (juxt :id :direction)

        result
        (group-by f parsed-urls)]

    (doseq [[[id direction] items] result]
      (when (-> items count (> 1))
        (let [urls
              (for [item items]
                (-> item ^URL (:url) (.getFile)))]

          (pg/error! "Migration %s has %s %s files: %s"
                     id
                     (count items)
                     (name direction)
                     (str/join ", " urls))))))

  parsed-urls)


(defn url->migrations [^URL url]
  (->> url
       (fs/url->children)
       (map parse-url)
       (filter some?)
       (validate-duplicates!)
       (group-parsed-urls)))


(defn read-disk-migrations [^String path]
  (->> path
       (fs/path->url)
       (url->migrations)))


(defn get-applied-migration-ids [conn table]
  (let [query
        {:select [:id]
         :from table
         :order-by [[:id :asc]]}]
    (->> query
         (pgh/query conn)
         (map :id)
         (set)
         (not-empty))))


(defn validate-conflicts! [migrations]
  (doseq [i (range 0 (-> migrations count dec))]
    (let [migration1 (get migrations i)
          migration2 (get migrations (inc i))]
      (when (and (-> migration1 :applied? not)
                 (-> migration2 :applied?))
        (pg/error! "Migration conflict: migration %s has been applied before %s"
                   (:id migration2)
                   (:id migration1))))))


(defn make-scope [config]

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


(defn -migrate [scope id-migration's]

  (let [{:keys [config
                migrations-table]}
        scope

        {:keys [verbose?]}
        config]

    (pg/with-connection [conn config]

      (doseq [[id migration]
              id-migration's]

        (log/infof "Processing next migration %s" id)

        (let [{:keys [slug url-next]}
              migration

              sql
              (some-> url-next slurp)]

          (when sql
            (when verbose?
              (log/infof sql))
            (pg/query conn sql))
          (pgh/insert-one conn
                          migrations-table
                          {:id id :slug slug}))))))


(defn migrate-to [config id-to]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (subseq migrations > id-current <= id-to)]

    (-migrate scope pending-migrations)))


(defn migrate-all [config]
  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (subseq migrations > id-current)]

    (-migrate scope pending-migrations)))


(defn migrate-one [config]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (take 1 (subseq migrations > id-current))]

    (-migrate scope pending-migrations)))


(defn -rollback [scope id-migration's]

  (let [{:keys [config
                migrations-table]}
        scope

        {:keys [verbose?]}
        config]

    (pg/with-connection [conn config]

      (doseq [[id migration]
              id-migration's]

        (log/infof "Processing prev migration %s" id)

        (let [{:keys [slug
                      url-prev]}
              migration

              sql
              (some-> url-prev slurp)]

          (when sql
            (when verbose?
              (log/infof sql))
            (pg/query conn sql))
          (pgh/delete conn
                      migrations-table
                      {:where [:= :id id]}))))))


(defn rollback-to [config id-to]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (rsubseq migrations > id-to <= id-current)]

    (-rollback scope pending-migrations)))


(defn rollback-all [config]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (rsubseq migrations <= id-current)]

    (-rollback scope pending-migrations)))


(defn rollback-one [config]

  (let [scope
        (make-scope config)

        {:keys [migrations
                id-current]}
        scope

        pending-migrations
        (take 1 (rsubseq migrations <= id-current))]

    (-rollback scope pending-migrations)))
