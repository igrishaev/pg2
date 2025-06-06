(defproject com.github.igrishaev/pg2-hugsql "0.1.41-SNAPSHOT"

  :description
  "HugSQL wrapper for PG2"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg2-core]
   [com.layerware/hugsql-core]
   [com.layerware/hugsql-adapter]]

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
