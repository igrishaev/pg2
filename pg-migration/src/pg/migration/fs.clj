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
   [clojure.java.io :as io]
   [clojure.string :as str]))


(defmacro error! [template & args]
  `(throw (new Error (format ~template ~@args))))


(defn path->url
  "
  Turn a string path into a URL: either a resoure
  "
  ^URL [^String path]
  (or (io/resource path)
      (let [file (io/file path)]
        (when (.exists file)
          (.toURL file)))
      (error! "Neither a resource nor a local file exists: %s " path)))


(defn url->dir ^File [^URL url]
  (let [file (io/as-file url)]
    (if (.isDirectory file)
      file
      (error! "The path %s is not a directory: %s" url))))


(defmulti url->children
  "
  Get child URLs for a given URL. The result
  doesn't include the given URL.
  "
  (fn [^URL url]
    (.getProtocol url)))


(defmethod url->children :default
  [url]
  (error! "Unsupported URL: %s" url))


(defmethod url->children "resource"
  [^URL url]
  (error! "It looks like you're trying to read migration resources from a GraalVM-compiled file, which is not possible at the moment. Please point the migration path to a local directory, not a resource. Sorry but I cannot do anything about it. The current URL is %s" url))


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
