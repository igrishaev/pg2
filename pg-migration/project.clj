(defproject com.github.igrishaev/pg2-migration "0.1.5-SNAPSHOT"

  :description
  "Migration utilities for PG2"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg2-core]
   [com.github.igrishaev/pg2-honey]]

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
   {:dependencies
    []}})
