(def MIN_JAVA_VERSION "16")

(defproject com.github.igrishaev/pg2-core "0.1.3-SNAPSHOT"

  :description
  "Postgres client in pure Java (no JDBC)"

  :scm {:dir ".."}

  :plugins
  [[lein-parent "0.3.8"]]

  :dependencies
  [[org.clojure/clojure]
   [metosin/jsonista]
   [less-awful-ssl]]

  :pom-addition
  [:properties
   ["maven.compiler.source" ~MIN_JAVA_VERSION]
   ["maven.compiler.target" ~MIN_JAVA_VERSION]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"
                  "-Xlint:preview"
                  "--release" ~MIN_JAVA_VERSION]

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
    [[org.clojure/data.csv]
     [com.github.igrishaev/pg2-honey]
     [com.github.igrishaev/pg2-component]]}})
