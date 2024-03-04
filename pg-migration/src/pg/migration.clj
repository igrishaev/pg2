(ns pg.migration
  (:import
   java.io.File
   java.lang.AutoCloseable
   java.lang.System$Logger
   java.lang.System$Logger$Level
   java.net.JarURLConnection
   java.net.URI
   java.net.URL
   java.time.OffsetDateTime
   java.time.ZoneOffset
   java.time.format.DateTimeFormatter
   java.util.jar.JarFile
   org.pg.Connection)
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :as sql]
   [pg.core :as pg]
   [pg.honey :as pgh]))


;; TODO
;; - generate migration id
;; - generate migration file
;; - zipfile support
;; - check logs in console
;; - docstrings
;; - demo
;; - docs
;; - cmd line args support
;; - lein plugin

;; tests
;; check jar file

(def DEFAULTS
  {:migrations-table :migrations
   :migrations-path "migrations"})


(def RE_FILE
  #"(?i)^(\d+)(.*?)\.(prev|next)\.sql$")


(def ^System$Logger LOGGER
  (System/getLogger "pg.migration"))


(defmacro log [template & args]
  `(.log LOGGER
         System$Logger$Level/INFO
         (format ~template ~@args)))


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
   (create-migration-files migrations-path ""))

  ([migrations-path slug]
   (let [id
         (generate-datetime-id)

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


(defn validate-duplicates! [parsed-files]
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
       (validate-duplicates!)
       (group-parsed-files)))


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
            (read-file-migrations migrations-path)

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
        scope]

    (pg/with-connection [conn config]

      (doseq [[id migration]
              id-migration's]

        (log "Processing next migration %s" id)

        (let [{:keys [slug file-next]}
              migration

              sql
              (some-> file-next slurp)]

          (when sql
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
        scope]

    (pg/with-connection [conn config]

      (doseq [[id migration]
              id-migration's]

        (log "Processing prev migration %s" id)

        (let [{:keys [slug
                      file-prev]}
              migration

              sql
              (some-> file-prev slurp)]

          (when sql
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



#_
(defn -main [& _]
  (-> "migrations"
      io/resource
      .toURI
      FileSystems/newFileSystem
      println))


#_
(comment

  (def -url
    (new URL "jar:file:/Users/ivan/work/pg2/pg-migration/target/pg2-migration-0.1.5-SNAPSHOT-standalone.jar!/migrations"))

  (def -conn
    (.openConnection -url))

  (def -jar
    (.getJarFile -conn))

  (def -entries
    (.entries -jar))

  (.getName (.nextElement -entries))

  (def -entry
    (.nextElement -entries))

  (.getInputStream -jar -e)

  ;; zipFile.getInputStream(zipEntry);

  (def -e
    (.getJarEntry -conn))

  ;; (.isDirectory -e)

  (.getInputStream -jar -entry)

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
