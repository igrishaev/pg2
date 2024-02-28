(ns pg.migration
  (:import java.io.File)
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [pg.core :as pg]
   [pg.honey :as pgh]
   [honey.sql :as sql]
   [clojure.java.io :as io]))


(defrecord Migration [id
                      slug
                      applied?
                      file-next
                      file-prev])


(defprotocol IRegistry

  (-apply-next [this])

  (-apply-prev [this])

  (-next-to [this id])

  (-prev-to [this id])

  (-next-all [this])

  (-prev-all [this]))


(defrecord Registry [conn
                     index
                     migrations
                     migrations-table
                     migrations-path]

  IRegistry

  (-apply-next [this]

    (let [migration
          (get migrations (inc index))]

      (cond

        (nil? migration)
        this

        (:applied? migration)
        (-> this
            (update :index inc))

        :else
        (let [{:keys [id
                      slug
                      file-next]}

              migration

              sql
              (slurp file-next)]

          (pg/query conn sql)
          (pgh/insert conn migrations-table {:id id :slug slug})

          (-> this
              (update :index inc)
              (update :migrations index assoc :applied? true))))))


  (-apply-prev [this]

    (let [migration
          (get migrations index)]

      (cond

        (nil? migration)
        this

        (:applied? migration)
        (-> this
            (update :index dec))

        :else
        (let [{:keys [id
                      file-prev]}
              migration

              sql
              (slurp file-prev)

              index-prev
              (dec index)

              migrations
              (update migrations index assoc :applied? false)]

          (pg/query conn sql)
          (pgh/delete conn migrations-table {:where [:= :id id]})

          (-> this
              (update :index dec)
              (update :migrations index assoc :applied? false))))))

  (-next-to [this id])

  (-prev-to [this id])

  (-next-all [this]
    (let [times
          (range (inc index) (count migrations))]
      (reduce
       (fn [THIS n]
         (-apply-next THIS))
       this
       times)))

  (-prev-all [this]
    (let [times
          (range 0 index)]
      (reduce
       (fn [THIS n]
         (-apply-prev THIS))
       this
       times))))


(def QUERY_TABLE
  {:create-table [:migrations :if-not-exists]
   :with-columns
   [[:id :bigint :primary-key]
    [:slug :text]
    [:created_at [:raw "timestamp with time zone not null default current_timestamp"]]]})


(defn create-migrations-table [conn]

  )

(def RE_FILE
  #"(?i)^(\d+)(.*?)\.(prev|next)\.sql$")


(defn migration-file? [^File file])



(defn cleanup-slug ^String [^String slug]
  (-> slug
      (str/replace #"-|_" " ")
      (str/replace #"\s+" " ")
      (str/trim)))


(defn file->migration [^File file]
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
       :next? (= direction :next)
       :prev? (= direction :prev)
       :direction direction
       :file file})))


(defn list-file-migrations [^String path]
  (->> path
       io/resource
       io/file
       file-seq
       (map file->migration)
       (filter some?)
       (map (juxt :id identity))
       (into (sorted-map))))


(defn list-db-migrations [conn]
  (let [query
        {:select [:*]
         :from :migrations
         :order-by [[:id :asc]]}

        rows
        (pgh/query conn query)]

    (->> rows
         (map (juxt :id identity))
         (into (sorted-map)))))


(defn get-pending-migration-ids [file-migrations
                                 db-migrations]
  (let [result
        (set/difference (-> file-migrations keys set)
                        (-> db-migrations keys set))]
    (-> result
        sort
        vec
        not-empty)))


(defn get-last-migration []
  )


(defn apply-migration [conn db-migration]

  (let [{:keys [id slug file prev? next?]}
        db-migration

        sql
        (slurp file)]

    (pg/query conn sql)

    (cond
      next?
      (pgh/insert conn :migrations {:id id :slug slug})
      prev?
      (pgh/delete conn :migrations {:where [:= :id id]}))))


(defn migrate-next-ids [conn migration-ids db-migrations]

  (doseq [migration-id
          migration-ids]

    (let [db-migration
          (get db-migrations migration-id)]

      (apply-migration conn db-migration))))


(defn migrate [config]

  (pg/with-connection [conn config]

    (let [db-migrations
          (list-db-migrations conn)

          file-migrations
          (list-file-migrations "migrations")

          pending-migration-ids
          (get-pending-migration-ids file-migrations
                                     db-migrations)]

      (migrate-next-ids conn pending-migration-ids db-migrations))))


(defn rollback []
  )
