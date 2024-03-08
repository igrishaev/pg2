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
  Turn a string path into a URL.
  "
  ^URL [^String path]
  (or (io/resource path)
      (error! "Resource %s doens't exist" path)))


(defmulti url->children
  "
  Get child URLs for a given URL. The result
  desn't unclude the given URL.
  "
  (fn [^URL url]
    (.getProtocol url)))


(defmethod url->children :default
  [url]
  (error! "Unsupported URL: %s" url))


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
