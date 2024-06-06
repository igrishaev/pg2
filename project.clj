(defproject _ "0.1.14-SNAPSHOT"

  :url
  "https://github.com/igrishaev/pg2"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :deploy-repositories
  {"releases"
   {:url "https://repo.clojars.org"
    :creds :gpg}
   "snapshots"
   {:url "https://repo.clojars.org"
    :creds :gpg}}

  :release-tasks
  [["vcs" "assert-committed"]
   ["sub" "change" "version" "leiningen.release/bump-version" "release"]
         ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["sub" "with-profile" "uberjar" "install"]
   ["sub" "with-profile" "uberjar" "deploy"]
   ["sub" "change" "version" "leiningen.release/bump-version"]
         ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :plugins
  [[lein-sub "0.3.0"]
   [exoscale/lein-replace "0.1.1"]]

  :dependencies
  []

  :sub
  ["pg-core"
   "pg-honey"
   "pg-component"
   "pg-migration"
   "pg-hugsql"]

  :managed-dependencies
  [[com.github.igrishaev/pg2-core :version]
   [com.github.igrishaev/pg2-honey :version]
   [com.github.igrishaev/pg2-component :version]
   [com.github.igrishaev/pg2-migration :version]
   [com.github.igrishaev/pg2-hugsql :version]
   [com.layerware/hugsql-core "0.5.3"]
   [com.layerware/hugsql-adapter "0.5.3"]
   [org.clojure/clojure "1.11.1"]
   [org.clojure/tools.cli "1.1.230"]
   [com.github.seancorfield/honeysql "2.4.1078"]
   [less-awful-ssl "1.0.6"]
   [com.github.seancorfield/next.jdbc "1.2.796"]
   [org.postgresql/postgresql "42.2.18"]
   [hikari-cp "3.0.1"]
   [com.stuartsierra/component "1.1.0"]
   [org.clojure/data.csv "1.0.1"]
   [metosin/jsonista "0.3.8"]
   [criterium "0.4.6"]
   [ring/ring-json "0.5.1"]
   [ring/ring-core "1.12.1"]
   [ring/ring-jetty-adapter "1.12.1"]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure]]

    :global-vars
    {*warn-on-reflection* true
     *assert* true}}})
