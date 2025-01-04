(ns pg.migration.cli
  "
  Command line interface for migrations.
  "
  (:gen-class)
  (:import
   java.io.File
   java.io.PushbackReader)
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]
   [pg.migration.err :refer [throw! rethrow!]]
   [pg.migration.fs :as fs]
   [pg.migration.core :as mig]
   [pg.migration.log :as log]))


(defn parse-int [string]
  (Integer/parseInt string))


(def edn-readers
  {'env
   (fn [v]
     (let [vname (name v)]
       (or (System/getenv vname)
           (throw! "Env variable %s is not set" vname))))})


(defn path-exists? ^Boolean [^String path]
  (-> path io/file .exists))


(defn load-config [path]
  (let [url (fs/path->url path)]
    (try
      (->> url
           io/reader
           (new PushbackReader)
           (edn/read {:readers edn-readers}))
      (catch Exception e
        (rethrow! e "Failed read an EDN config: %s" url)))))


(defn current-user [_]
  (or (System/getenv "USER") ""))


(def CLI-OPT-MAIN

  [["-c" "--config CONFIG" "Path to an .edn config (a resource or a local file)"
    :id :config-path]

   ["-p" "--port PORT" "Port number"
    :id :port
    :default 5432
    :parse-fn parse-int]

   ["-h" "--host HOST" "Host name"
    :id :host
    :default "localhost"]

   ["-u" "--user USER" "User"
    :id :user
    :default-desc "The current USER env var"
    :default-fn current-user]

   ["-w" "--password PASSWORD" "Password"
    :id :password
    :default-desc "<empty string>"
    :default ""]

   ["-d" "--database DATABASE" "Database"
    :id :database
    :default-desc "The current USER env var"
    :default-fn current-user]

   [nil "--table TABLE" "Migrations table"
    :id :migrations-table
    :parse-fn keyword
    :default mig/MIG_TABLE]

   [nil "--path PATH" "Migrations path (a resource or a local file)"
    :id :migrations-path
    :default mig/MIG_PATH]])


(def OPT-HELP
  [nil "--help" "Show help message"
   :id :help?
   :default false])


(def CLI-OPT-MIGRATE
  [[nil "--all" "Migrate all the pending migrations"
    :id :all?]

   [nil "--one" "Migrate next a single pending migration"
    :id :one?]

   [nil "--to ID" "Migrate next to certain migration"
    :id :to
    :parse-fn parse-int]

   OPT-HELP])


(def CLI-OPT-ROLLBACK
  [[nil "--all" "Rollback all the previous migrations"
    :id :all?]

   [nil "--one" "Rollback to the previous migration"
    :id :one?]

   [nil "--to ID" "Rollback to certain migration"
    :id :to
    :parse-fn parse-int]

   OPT-HELP])


(def CLI-OPT-CREATE
  [[nil "--id ID" "The id of the migration (auto-generated if not set)"
    :id :id
    :parse-fn parse-int]

   [nil "--slug SLUG" "Optional slug (e.g. 'create-users-table')"
    :id :slug]

   OPT-HELP])


(defn render-erros
  "
  Print CLI-related error messages.
  "
  [errors]
  (println "Errors:")
  (doseq [err errors]
    (println " -" err)))


(defn render-summary [summary]
  (println "Syntax:")
  (println summary))


(defn exit
  "
  Terminate a program with a given status code
  and an error message. The message gets sent to either
  stdout or stderr depending on the status code.
  "
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


(defmacro with-exit
  "
  A macro that prevents terminating JVM
  in favour of throwing an exception.
  "
  [& body]
  `(with-redefs [exit
                 (fn [& _#]
                   (throw (new Error "exit")))]
     ~@body))


(defn parse-args
  "
  A strict version of `parse-opts` that renders errors
  and terminates the program.
  "
  [args cli-opt]

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


(defn handle-migrate
  "
  Handle the `migrate` sub-command.
  "
  [config cmd-args]
  (let [parsed
        (parse-args cmd-args
                    CLI-OPT-MIGRATE)

        {:keys [options
                summary]}
        parsed

        {:keys [help? all? one? to]}
        options]

    (cond

      help?
      (render-summary summary)

      one?
      (mig/migrate-one config)

      to
      (mig/migrate-to config to)

      all?
      (mig/migrate-all config)

      :default
      (mig/migrate-all config))))


(defn handle-rollback
  "
  Handle the `rollback` sub-command.
  "
  [config cmd-args]

  (let [parsed
        (parse-args cmd-args
                    CLI-OPT-ROLLBACK)

        {:keys [options
                summary]}
        parsed

        {:keys [help? all? one? to]}
        options]

    (cond

      help?
      (render-summary summary)

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
  "
  Handle the `create` sub-command.
  "
  [config cmd-args]

  (let [{:keys [options
                summary]}
        (parse-args cmd-args CLI-OPT-CREATE)

        {:keys [id
                slug
                help?]}
        options

        {:keys [migrations-path]}
        config]

    (cond

      help?
      (render-summary summary)

      :else
      (let [[file-prev file-next]
            (mig/create-migration-files migrations-path
                                        {:id id :slug slug})]
        (println (str file-prev))
        (println (str file-next))))))


(defn handle-unknown-cmd [cmd]
  (println "Supported commands:")
  (doseq [cmd (sort CMDS)]
    (println " -" cmd)))


(defn handle-list
  "
  Handle the `list` sub-command.
  "
  [config cmd-args]
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
             (mig/letters (- id-max-size 3)) "ID"
             |
             "Applied?"
             |
             "Slug")

    (println |
             (mig/letters id-max-size \-)
             |
             (mig/letters 8 \-)
             |
             (mig/letters 8 \-))

    (doseq [[id migration]
            migrations]

      (let [{:keys [slug
                    url-next
                    url-prev
                    applied?]}
            migration

            applied-str
            (->> applied?
                 (boolean)
                 (format "%-8s"))]

        (println |
                 (format id-template id)
                 |
                 applied-str
                 |
                 slug)))))


(def CONFIG-FIELDS
  [:user
   :database
   :host
   :port
   :password
   :migrations-table
   :migrations-path])


(defn main
  "
  A more convenient version of -main that accepts
  a vector of args plus extra args.
  "
  [args & more]

  (let [args-all
        (-> [] (into args) (into more))

        parsed
        (parse-args args-all
                    CLI-OPT-MAIN)

        {:keys [options
                arguments
                summary]}
        parsed

        {:keys [config-path]}
        options

        config
        (some-> config-path
                load-config)

        config-full
        (-> options
            (select-keys CONFIG-FIELDS)
            (merge config))

        [cmd & cmd-args]
        arguments]

    (cond

      (= cmd CMD_HELP)
      (handle-help summary)

      (= cmd CMD_MIGRATE)
      (handle-migrate config-full cmd-args)

      (= cmd CMD_ROLLBACK)
      (handle-rollback config-full cmd-args)

      (= cmd CMD_CREATE)
      (handle-create config-full cmd-args)

      (= cmd CMD_LIST)
      (handle-list config-full cmd-args)

      :else
      (handle-unknown-cmd cmd))))


(defn -main [& args]
  (main args))
