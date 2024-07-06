(defproject com.github.igrishaev/pg2-component "0.1.15"

  :description
  "Component wrapper for PG2"

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg2-core]
   [com.stuartsierra/component]]

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
