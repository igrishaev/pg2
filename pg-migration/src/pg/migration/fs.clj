(ns pg.migration.fs
  (:import
   java.io.File
   java.net.JarURLConnection
   java.net.URL
   java.util.jar.JarEntry)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [pg.core :as pg]))


(defn path->url ^URL [^String path]
  (or (io/resource path)
      (pg/error! "Resource %s doens't exist" path)))


(defmulti get-children
  (fn [^URL url]
    (.getProtocol url)))


(defmethod get-children :default
  [url]
  (pg/error! "Unsupported URL: %s" url))


(defmethod get-children "file"
  [^URL url]
  (->> url
       io/as-file
       file-seq
       (remove (fn [^File file]
                 (.isDirectory file)))
       (map (fn [^File file]
              (-> file .toURI .toURL)))))


(defmethod get-children "jar"
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
