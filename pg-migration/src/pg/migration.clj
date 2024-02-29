(ns pg.migration
  (:gen-class)
  (:import
   java.net.URI
   java.nio.file.FileSystem
   java.nio.file.FileSystems
   java.nio.file.Files
   java.lang.System$Logger
   java.lang.System$Logger$Level
   java.io.File
   java.lang.AutoCloseable)
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :as sql]
   [pg.core :as pg]
   [pg.honey :as pgh]))


(def ^System$Logger LOGGER
  (System/getLogger "pg.migration"))


(defmacro log [template & args]
  `(.log LOGGER
         System$Logger$Level/INFO
         (format ~template ~@args)))


(def RE_FILE
  #"(?i)^(\d+)(.*?)\.(prev|next)\.sql$")


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



(defn parse-file [^File file]
  (when-let [[_ id-raw slug-raw direction-raw]
             (re-matches RE_FILE (.getName file))]

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
       :file file})))


(defn group-parsed-files [parsed-files]
  (reduce
   (fn [acc {:keys [id slug direction file]}]
     (let [node
           (cond-> {:id id
                    :slug slug}

             (= direction :prev)
             (assoc :file-prev file)

             (= direction :next)
             (assoc :file-next file))]

       (update acc id merge node)))
   (sorted-map)
   parsed-files))


(defn is-directory? [^File file]
  (.isDirectory file))


(defn validate-parsed-files [parsed-files]
  (let [f
        (juxt :id :direction)

        result
        (group-by f parsed-files)]

    (doseq [[[id direction] items] result]
      (when (-> items count (> 1))
        (let [files
              (for [item items]
                (-> item ^File (:file) .getName))]
          (pg/error! "Migration %s has %s %s files: %s"
                     id
                     (count items)
                     (name direction)
                     (str/join ", " files))))))

  parsed-files)


(defn read-file-migrations [^String path]
  (->> path
       (io/resource)
       (io/file)
       (file-seq)
       (remove is-directory?)
       (map parse-file)
       (filter some?)
       (validate-parsed-files)
       (group-parsed-files)))


(defn get-applied-migration-ids [conn]
  (let [query
        {:select [:id]
         :from :migrations
         :order-by [[:id :asc]]}]
    (->> query
         (pgh/query conn)
         (map :id)
         (set)
         (not-empty))))


(def DEFAULTS
  {:migrations-table :migrations
   :migrations-path "migrations"})



(defn -main [& _]
  (-> "migrations"
      io/resource
      .toURI
      FileSystems/newFileSystem
      println))


(defn check-migration-conflicts [migrations]
  (doseq [i (range 0 (-> migrations count dec))]
    (let [migration1 (get migrations i)
          migration2 (get migrations (inc i))]
      (when (and (-> migration1 :applied? not)
                 (-> migration2 :applied?))
        (pg/error! "Migration conflict: migration %s has been applied before %s"
                   (:id migration2)
                   (:id migration1))))))


(defn PREPARE [config]

  (let [config+
        (merge DEFAULTS config)

        {:keys [migrations-table
                migrations-path]}
        config+

        conn
        (pg/connect config+)

        _
        (ensure-table conn migrations-table)

        migrations
        (read-file-migrations migrations-path)

        applied-ids-set
        (get-applied-migration-ids conn)

        id-current
        (apply max -1 applied-ids-set)

        migrations+
        (reduce-kv
         (fn [acc id migration]
           (let [flag (contains? applied-ids-set id)]
             (assoc-in acc [id :applied?] flag)))
         migrations
         migrations)]

    (check-migration-conflicts (vals migrations+))

    {:conn conn
     :migrations migrations+
     :id-current id-current
     :migrations-table migrations-table
     :migrations-path migrations-path}))


(defn MIGRATE-TO [config id-to]

  (let [{:keys [conn
                migrations
                id-current
                migrations-table]}
        (PREPARE config)

        pending-migrations
        (subseq migrations > id-current <= id-to)]

    (doseq [[id migration]
            pending-migrations]

      (log "Processing next migration %s" id)

      (let [{:keys [slug
                    file-next]}
            migration

            sql
            (some-> file-next slurp)]

        (when file-next
          (pg/query conn sql)
          (pgh/insert-one conn
                          migrations-table
                          {:id id :slug slug}))))))


(defn MIGRATE [config]
  (MIGRATE-TO config Long/MAX_VALUE))


(defn ROLLBACK-TO [config id-to]

  (let [{:keys [conn
                migrations
                id-current
                migrations-table]}
        (PREPARE config)

        pending-migrations
        (rsubseq migrations > id-to <= id-current)]

    (doseq [[id migration]
            pending-migrations]

      (log "Processing prev migration %s" id)

      (let [{:keys [slug
                    file-prev]}
            migration

            sql
            (some-> file-prev slurp)]

        (when file-prev
          (pg/query conn sql)
          (pgh/delete conn
                      migrations-table
                      {:where [:= :id id]}))))))


(defn ROLLBACK [config]
  (ROLLBACK-TO config Long/MIN_VALUE))




#_
(comment

  (def -config
    {:host "127.0.0.1"
     :port 10150
     :user "test"
     :password "test"
     :database "test"})

  (def -conn
    (pg/connect -config))

  (ensure-table -conn :migrations)

  (read-file-migrations "migrations")

  (get-applied-migration-ids -conn)

  )
