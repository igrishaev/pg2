(defproject com.github.igrishaev/pg2-bench "0.1.0-SNAPSHOT"

  :description
  "Various benchmarks"

  :scm {:dir ".."}

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[com.github.igrishaev/pg2-core]
   [com.github.igrishaev/pg2-honey]
   [com.github.igrishaev/pg2-hugsql]
   [metosin/jsonista]
   [com.github.seancorfield/next.jdbc]
   [org.postgresql/postgresql]
   [org.clojure/data.csv]
   [hikari-cp]
   [criterium]
   [ring/ring-json]
   [ring/ring-core]
   [ring/ring-jetty-adapter]]

  :main pg.bench

  :parent-project
  {:path "../project.clj"
   :inherit [:deploy-repositories
             :license
             :release-tasks
             :managed-dependencies
             :plugins
             :repositories
             :url
             [:profiles :dev]]})
