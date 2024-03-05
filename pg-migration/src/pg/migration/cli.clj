(ns pg.migration.cli
  (:gen-class)
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [pg.migration.core :as mig]
   [pg.migration.log :as log]
   )
  )


(def cli-options

  [["-p" "--port PORT" "Port number"
    :default 5432
    :parse-fn #(Integer/parseInt %)]

   ["-h" "--host HOST" "Host name"
    :default "localhost"]

   ["-u" "--user USER" "Database user"]

   ["-w" "--password PASSWORD" "Database password"]

   ["-d" "--database DATABASE" "Database name"]

   ["--migrations-table TABLE" "Migrations table"
    :parse-fn keyword
    :default :migrations]

   ["--migrations-path PATH" "Migrations path"
    :default "migrations"]])


;; ["-p" "3333" "migrate"]
;; ["-p" "3333" "migrate" "--all"]
;; ["-p" "3333" "migrate" "--one"]
;; ["-p" "3333" "migrate" "--to"]

;; ["-p" "3333" "rollback"]
;; ["-p" "3333" "rollback" "--all"]
;; ["-p" "3333" "rollback" "--one"]
;; ["-p" "3333" "rollback" "--to"]

;; ["-p" "3333" "create" "--id" "--slug"]
;; ["-p" "3333" "list"]
;; ["-p" "3333" "applied"]


(defn -main [& args]

  (let [parsed
        (parse-opts args cli-options)]

    parsed



    )




  )
