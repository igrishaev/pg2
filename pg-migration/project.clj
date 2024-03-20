(defproject com.github.igrishaev/pg2-migration "0.1.6"

  :description
  "Migration utilities for PG2"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg2-core]
   [com.github.igrishaev/pg2-honey]
   [org.clojure/tools.cli]]

  :main pg.migration.cli

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :release-tasks
             :managed-dependencies
             :plugins
             :repositories
             :url
             [:profiles :dev]]}

  :profiles
  {:test
   {:resource-paths ["test/resources"]
    :dependencies
    []}})
