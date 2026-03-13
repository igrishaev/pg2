(ns pg.keys
  "
  A namespace for functions to process DB keys.
  "
  (:require
   [clojure.string :as str])
  (:import
   clojure.lang.Keyword))

(defn ->kebab
  "
  Turn a string column name into into a kebab-case
  formatted keyword.
  "
  ^Keyword [^String column]
  (-> column (str/replace #"_" "-") keyword))
