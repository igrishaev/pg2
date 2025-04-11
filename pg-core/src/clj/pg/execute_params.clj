(ns pg.execute-params
  "
  A dedicated namespace to build an instance of the
  `ExecuteParams` class from a Clojure map.
  "
  (:require
   [clojure.string :as str]
   [pg.fold :as fold])
  (:import
   clojure.lang.Keyword
   java.util.List
   java.util.Map
   org.pg.ExecuteParams))


(defn ->kebab
  "
  Turn a string column name into into  a kebab-case
  formatted keyword.
  "
  ^Keyword [^String column]
  (-> column (str/replace #"_" "-") keyword))


(defn ->execute-params
  "
  Make an instance of ExecuteParams from a Clojure map.
  "
  ^ExecuteParams [^Map opt]

  (if (or (nil? opt) (= opt {}))
    ExecuteParams/INSTANCE

    (let [{:keys [^List params
                  oids

                  ;; portal
                  max-rows

                  ;; keys
                  kebab-keys?
                  fn-key

                  ;; streams
                  output-stream
                  input-stream

                  ;; fold/reduce
                  as
                  first
                  first? ;; for backward compatibility
                  map
                  index-by
                  group-by
                  kv
                  java
                  run
                  column
                  columns
                  table
                  to-edn
                  to-json
                  reduce
                  into

                  ;; format
                  binary-encode?
                  binary-decode?

                  ;; copy csv
                  csv-null
                  csv-sep
                  csv-end

                  ;; copy general
                  copy-buf-size
                  ^CopyFormat copy-format
                  copy-csv?
                  copy-bin?
                  copy-tab?
                  copy-in-rows
                  copy-in-maps
                  copy-in-keys]}
          opt]

      (cond-> (ExecuteParams/builder)

        params
        (.params params)

        oids
        (.oids oids)

        max-rows
        (.maxRows max-rows)

        fn-key
        (.fnKeyTransform fn-key)

        output-stream
        (.outputStream output-stream)

        input-stream
        (.inputStream input-stream)

        kebab-keys?
        (.fnKeyTransform ->kebab)

        ;;
        ;; reducers
        ;;
        as
        (.reducer as)

        (or first first?)
        (.reducer fold/first)

        map
        (.reducer (fold/map map))

        index-by
        (.reducer (fold/index-by index-by))

        group-by
        (.reducer (fold/group-by group-by))

        kv
        (.reducer (fold/kv (clojure.core/first kv) (second kv)))

        run
        (.reducer (fold/run run))

        column
        (.reducer (fold/column column))

        columns
        (.reducer (fold/columns columns))

        table
        (.reducer (fold/table))

        java
        (.reducer fold/java)

        to-edn
        (.reducer (fold/to-edn to-edn))

        to-json
        (.reducer (fold/to-json to-json))

        reduce
        (.reducer (fold/reduce (clojure.core/first reduce)
                               (second reduce)))

        into
        (.reducer (fold/into (clojure.core/first into)
                             (second into)))

        ;; end reducers

        (some? binary-encode?)
        (.binaryEncode binary-encode?)

        (some? binary-decode?)
        (.binaryDecode binary-decode?)

        csv-null
        (.CSVNull csv-null)

        csv-sep
        (.CSVCellSep csv-sep)

        csv-end
        (.CSVLineSep csv-end)

        copy-csv?
        (.setCSV)

        copy-bin?
        (.setBin)

        copy-tab?
        (.setBin)

        copy-format
        (.copyFormat copy-format)

        copy-buf-size
        (.copyBufSize copy-buf-size)

        copy-in-rows
        (.copyInRows copy-in-rows)

        copy-in-maps
        (.copyInMaps copy-in-maps)

        copy-in-keys
        (.copyInKeys copy-in-keys)

        :finally
        (.build)))))
