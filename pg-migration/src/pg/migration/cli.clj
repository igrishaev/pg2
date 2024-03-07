(ns pg.migration.cli
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]
   [pg.migration.core :as mig]
   [pg.migration.log :as log]))


(defn parse-int [string]
  (Integer/parseInt string))


(def CLI-OPT-MAIN

  [["-p" "--port PORT" "Port number"
    :id :port
    :default 5432
    :parse-fn parse-int]

   ["-h" "--host HOST" "Host name"
    :id :host
    :default "localhost"]

   ["-u" "--user USER" "User"
    :id :user
    :default (System/getenv "USER")]

   ["-w" "--password PASSWORD" "Password"
    :id :password
    :default ""]

   ["-d" "--database DATABASE" "Database"
    :id :database
    :default (System/getenv "USER")]

   ["--migrations-table TABLE" "Migrations table"
    :id :migrations-table
    :parse-fn keyword
    :default :migrations]

   ["--migrations-path PATH" "Migrations path"
    :id :migrations-path
    :default "migrations"]])


(def CLI-OPT-MIGRATE
  [[nil "--one" "Migrate next a single pending migration"
    :id :one?]

   [nil "--to ID" "Migrate next to certain migration"
    :id :to
    :parse-fn parse-int]])


(def CLI-OPT-MIGRATE
  [[nil "--all" "Rollback all the previous migrations"
    :id :all?]

   [nil "--one" "Rollback to the previous migration"
    :id :one?]

   [nil "--to ID" "Rollback to certain migration"
    :id :to
    :parse-fn parse-int]

   ["-v" "--verbose" "Verbose (println SQL expressions)"
    :id :verbose?
    :default false]])


(def CLI-OPT-CREATE
  [[nil "--id ID" "The id of the migration (auto-generated if not set)"
    :id :id
    :parse-fn parse-int]

   [nil "--slug SLUG" "Optional slug (e.g. 'create-users-table')"
    :id :slug]

   ["-v" "--verbose" "Verbose (println SQL expressions)"
    :id :verbose?
    :default false]])


;; ["-p" "3333" "create" "--id" "--slug"]
;; ["-p" "3333" "list"]
;; ["-p" "3333" "applied"]



(defn render-erros [errors]
  (println "Errors:")
  (doseq [err errors]
    (println " -" err)))


(defn render-summary [summary]
  (println "Syntax:")
  (println summary))


(defn exit
  ([code]
   (exit code nil))

  ([code message & args]
   (exit code (apply format message args)))

  ([code ^String message]
   (let [channel
         (if (zero? code) *out* *err*)]
     (when message
       (binding [*out* channel]
         (println message))))
   (System/exit code)))


(defmacro with-exit [& body]
  `(with-redefs [exit
                 (fn [code# & [message# & args#]]
                   (throw (new Error (when message#
                                       (apply format message# args#)))))]
     ~@body))


(defn parse-args [args cli-opt]
  (let [{:as parsed
         :keys [errors
                summary]}
        (parse-opts args
                    cli-opt
                    :in-order true
                    :strict true)]
    (when errors
      (render-erros errors)
      (println)
      (render-summary summary)
      (exit 1))
    parsed))


(defn handle-migrate [config cmd-args]
  (let [parsed
        (parse-args cmd-args
                    CLI-OPT-MIGRATE)

        {:keys [options]}
        parsed

        {:keys [one? to]}
        options]

    (cond

      one?
      (mig/migrate-one config)

      to
      (mig/migrate-to config to)

      :default
      (mig/migrate-all config))))


(defn handle-rollback [config cmd-args]

  (let [parsed
        (parse-args cmd-args
                    CLI-OPT-ROLLBACK)

        {:keys [options]}
        parsed

        {:keys [all? one? to]}
        options]

    (cond

      all?
      (mig/rollback-all config)

      to
      (mig/rollback-to config to)

      :default
      (mig/rollback-one config))))



(def CMD_HELP "help")
(def CMD_MIGRATE "migrate")
(def CMD_ROLLBACK "rollback")
(def CMD_CREATE "create")
(def CMD_LIST "list")


(def CMDS #{CMD_HELP
            CMD_MIGRATE
            CMD_ROLLBACK
            CMD_CREATE
            CMD_LIST})


(defn handle-help [summary]
  (println "Manage migrations via CLI")
  (println)
  (println "Synatax:")
  (println)
  (println "<global options> <command> <command options>")
  (println)
  (println "Global options:")
  (println)
  (println summary)
  (println)
  (println "Supported commands:")
  (println)
  (doseq [cmd (sort CMDS)]
    (println " -" cmd))
  (println)
  (println "Command-specific help:")
  (println)
  (println "<command> --help")
  (println))


(defn handle-create
  [config cmd-args]

  (let [{:keys [options]}
        (parse-args cmd-args CLI-OPT-CREATE)

        {:keys [id
                slug]}
        options

        {:keys [migrations-path]}
        config]

    (mig/create-migration-files migrations-path
                                {:id id :slug slug})))


(defn handle-unknown-cmd [cmd]
  (println "Supported commands:")
  (doseq [cmd (sort CMDS)]
    (println " -" cmd)))


(defn letters
  ([n]
   (letters n \space))
  ([n ch]
   (str/join (repeat n ch))))


(defn handle-list [config cmd-args]
  (let [scope
        (mig/make-scope config)

        {:keys [id-current
                migrations]}
        scope

        id-max-size
        (->> migrations
             keys
             (apply max 4))

        | \|

        id-template
        (str \% id-max-size \d)]

    (println "Migrations:")
    (println)

    (println |
             (letters (- id-max-size 3)) "ID"
             |
             "Applied?"
             |
             "Slug")

    (println |
             (letters id-max-size \-)
             |
             (letters 8 \-)
             |
             (letters 8 \-))

    (doseq [[id migration]
            migrations]

      (let [{:keys [slug
                    url-next
                    url-prev
                    applied?]}
            migration]

        (println |
                 (format id-template id)
                 |
                 (if applied? "true    " "false   ")
                 |
                 slug)))))


(defn -main [& args]

  (let [{:keys [options
                arguments
                summary]}

        (parse-args args
                    CLI-OPT-MAIN)

        config
        (select-keys options
                     [:user
                      :database
                      :host
                      :port
                      :password
                      :migrations-table
                      :migrations-path
                      :verbose?])

        [cmd & cmd-args]
        arguments]

    (cond

      (= cmd CMD_HELP)
      (handle-help summary)

      (= cmd CMD_MIGRATE)
      (handle-migrate config cmd-args)

      (= cmd CMD_ROLLBACK)
      (handle-rollback config cmd-args)

      (= cmd CMD_CREATE)
      (handle-create config cmd-args)

      (= cmd CMD_LIST)
      (handle-list config cmd-args)

      :else
      (handle-unknown-cmd cmd))))
