(ns pg.json
  "
  JSON read & write shortcuts.
  "
  (:refer-clojure :exclude [read-string])
  (:import
   java.io.InputStream
   java.io.OutputStream
   java.io.Reader
   java.io.Writer
   org.pg.json.JSON))


(defn read-string
  "
  Parse JSON from a string.
  "
  [^String input]
  (JSON/readValue input))


(defn read-stream
  "
  Parse JSON from an input stream.
  "
  [^InputStream input]
  (JSON/readValue input))


(defn read-reader
  "
  Parse JSON from a reader.
  "
  [^Reader input]
  (JSON/readValue input))


(defn write-writer
  "
  Parse JSON from a Writer instance.
  "
  [value ^Writer writer]
  (JSON/writeValue writer value))


(defn write-stream
  "
  Encode JSON into the OutputStream.
  "
  [value ^OutputStream out]
  (JSON/writeValue out value))


(defn write-string
  "
  Encode JSON into a string.
  "
  ^String [value]
  (JSON/writeValueToString value))
