(ns pg.migration
  (:import
   java.io.File
   java.lang.AutoCloseable)
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :as sql]
   [pg.core :as pg]
   [pg.honey :as pgh]))


(defprotocol IMigration
  (-applied?
    [this]
    [this flag]))


(defrecord Migration [id
                      slug
                      applied?
                      file-next
                      file-prev]

  IMigration

  (-applied? [this]
    applied?)

  (-applied? [this flag]
    (assoc this :applied? flag)))


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
                     migrations-table]

  AutoCloseable

  (close [this]
    (pg/close conn))

  IRegistry

  (-apply-next [this]

    (let [migration
          (get migrations (inc index))]

      (cond

        (nil? migration)
        this

        (-applied? migration)
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
          (pgh/insert-one conn migrations-table {:id id :slug slug})

          (-> this
              (update :index inc)
              (update-in [:migrations index] -applied? true))))))


  (-apply-prev [this]

    (let [migration
          (get migrations index)]

      (cond

        (nil? migration)
        this

        (-applied? migration)
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
              (update-in [:migrations index] -applied? false)))

        :else
        (-> this
            (update :index dec)))))

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


(def RE_FILE
  #"(?i)^(\d+)(.*?)\.(prev|next)\.sql$")


(defn cleanup-slug ^String [^String slug]
  (-> slug
      (str/replace #"-|_" " ")
      (str/replace #"\s+" " ")
      (str/trim)))


(defn file->migration ^Migration [^File file]
  (when-let [[_ id-raw slug-raw direction-raw]
             (re-matches RE_FILE (.getName file))]

    (let [id
          (Long/parseLong id-raw)

          slug
          (cleanup-slug slug-raw)

          direction
          (-> direction-raw
              str/lower-case
              keyword)

          file-next
          (when (= direction :next)
            file)

          file-prev
          (when (= direction :prev)
            file)]

      (new Migration id slug nil file-next file-prev))))


(defn ensure-table [conn migrations-table]
  (let [query
        {:create-table [migrations-table :if-not-exists]
         :with-columns
         [[:id :bigint :primary-key]
          [:slug :text]
          [:created_at [:raw "timestamp with time zone not null default current_timestamp"]]]}]
    (pgh/query conn query)))


(defn index-by [f coll]
  (->> coll
       (map (juxt f identity))
       (into {})))


(defn enumerate [coll]
  (map-indexed vector coll))


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
   (fn [acc {:as foo
             :keys [id slug direction file]}]

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
  (not (.isDirectory file)))


(defn read-file-migrations [^String path]
  (->> path
       (io/resource)
       (io/file)
       (file-seq)
       (filter is-directory?)
       (map parse-file)
       (filter some?)
       (group-parsed-files)
       (vals)
       (mapv map->Migration)))


(defn get-applied-migration-ids [conn]
  (let [query
        {:select [:id]
         :from :migrations
         :order-by [[:id :asc]]}]
    (->> query
         (pgh/query conn)
         (map :id)
         (set))))


(def DEFAULTS
  {:migrations-table :migrations
   :migrations-path "migrations"})


(defn init-registry [config]

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

        id-max
        (when (seq applied-ids-set)
          (apply max applied-ids-set))

        migrations+
        (vec
         (for [{:as migration :keys [id]}
               migrations]
           (let [flag
                 (contains? applied-ids-set id)]
             (-applied? migration flag))))

        index
        (reduce
         (fn [_ [i migration]]
           (when (-> migration :id (= id-max))
             (reduced i)))
         nil
         (enumerate migrations+))

        index
        (or index -1)]

    (new Registry
         conn
         index
         migrations+
         migrations-table)))


#_
(comment

  (def -config
    {:host "127.0.0.1"
     :port 10150
     :user "test"
     :password "test"
     :database "test"})

  (def -reg
    (init-registry -config))

  (-apply-next -reg)
  (-apply-prev -reg)


  )
