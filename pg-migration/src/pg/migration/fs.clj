(ns pg.migration.fs
  "
  Utilities to handle both file and jar URLs.
  "
  (:import
   java.io.File
   java.net.JarURLConnection
   java.net.URL
   java.util.jar.JarEntry)
  (:require
   [pg.migration.err :refer [throw!]]
   [clojure.java.io :as io]
   [clojure.string :as str]))


(defn path->url
  "
  Turn a string path into a URL: either a resoure or a local file.
  The file is checked for existence. When nothing is found, return
  nil.
  "
  ^URL [^String path]
  (or (io/resource path)
      (let [file (io/file path)]
        (when (.exists file)
          (.toURL file)))))


(defn path->url!
  "
  An aggressive version of `path->url` what ends up with an exception
  should nothing is found.
  "
  ^URL [^String path]
  (or (path->url path)
      (throw! "Neither a resource nor a local file exists: %s" path)))


(defmulti url->children
  "
  Get child URLs for a given URL. The result
  doesn't include the given URL.
  "
  (fn [^URL url]
    (.getProtocol url)))


(defmethod url->children :default
  [url]
  (throw! "Unsupported URL: %s" url))


(defmethod url->children "resource"
  [^URL url]
  (throw! "It looks like you're trying to read migrations from a GraalVM-compiled file, which is not possible at the moment. Please point the migration path to a local directory, not a resource. Sorry but I cannot do anything about it. For debug: the current URL is %s" url))


(defmethod url->children "file"
  [^URL url]
  (->> url
       io/as-file
       file-seq
       (remove (fn [^File file]
                 (.isDirectory file)))
       (map (fn [^File file]
              (-> file .toURI .toURL)))))


(defmethod url->children "jar"
  [^URL url]

  (let [^JarURLConnection conn
        (.openConnection url)

        entry-name
        (-> conn
            .getJarEntry
            .getName)

        jar-file
        (.getJarFile conn)

        jar-path
        (.getName jar-file)

        entries
        (.entries jar-file)]

    (->> entries
         enumeration-seq
         (remove (fn [^JarEntry e]
                   (.isDirectory e)))
         (filter (fn [^JarEntry e]
                   (-> e
                       .getName
                       (str/starts-with? entry-name))))
         (map (fn [^JarEntry e]
                (new URL (format "jar:file:%s!/%s"
                                 jar-path (.getName e))))))))
