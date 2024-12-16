(ns pg.hugsql
  (:require
   [clojure.string :as str]
   [pg.core :as pg]
   [hugsql.core :as hugsql]
   [hugsql.parameters :as p]
   [hugsql.adapter :as adapter]))


(def ^:dynamic *$*
  "The current index of $1, ... $n placeholders."
  0)

(defn $next
  "
  Bump the index and produce a '$n' string.
  "
  []
  (set! *$* (inc *$*))
  (format "$%d" *$*))


(defn $wrap
  "
  Wrap a function into the binding macro to make
  the `$next` function work.
  "
  [f]
  (fn [& args]
    (binding [*$* *$*]
      (apply f args))))


;;
;; Here and below: override JDBC "?" placeholder with
;; native Postgres ones like $1, ... $n.
;;

(extend-type Object

  p/SQLVecParam
  (sqlvec-param [param data options]
    #bogus 1
    (println param
             data
             options
             (get-in data (p/deep-get-vec (:name param)))
             )
    (get-in data (p/deep-get-vec (:name param))))

  p/ValueParam

  (value-param [param data options]
    (let [value
          (get-in data (p/deep-get-vec (:name param)))]
      [($next) value]))

  p/ValueParamList

  (value-param-list [param data options]
    (let [coll
          (get-in data (p/deep-get-vec (:name param)))

          placeholders
          (str/join "," (for [_ coll] ($next)))]

      (into [placeholders] coll))))


(deftype PG2Adapter [defaults]

  adapter/HugsqlAdapter

  (execute [this db sqlvec options]

    (let [[sql & params]
          sqlvec

          {opt :pg}
          options

          opt-full
          (-> defaults
              (merge opt)
              (assoc :params params))]

      (pg/on-connection [conn db]
        (pg/execute conn sql opt-full))))

  (query [this db sqlvec options]
    (adapter/execute this db sqlvec options))

  (result-one [this result options]
    (first result))

  (result-many [this result options]
    result)

  (result-affected [this result options]
    (or (:inserted result)
        (:updated result)
        (:deleted result)
        (:selected result)
        (:copied result)))

  (result-raw [this result options]
    result)

  (on-exception [this exception]
    (throw exception)))


(defn make-adapter
  "
  Initiate an adapter object. The defaults, when specified,
  are passed to the `pg.core/execute` function.
  "
  ([]
   (new PG2Adapter nil))
  ([defaults]
   (new PG2Adapter defaults)))


(defn wrap-signature
  "
  Slightly correct the signature of a function
  produced by HugSQL. Namely, pass `pg.core/execute`
  parameters through a dedicated key to prevent
  merging them with something else.
  "
  [f]
  (fn
    ([db]
     (f db))
    ([db params]
     (f db params))
    ([db params opt]
     (f db params {:pg opt}))))


(defn intern-function
  "
  Inject a function produced by HugSQL
  into the current namespace.
  "
  [fn-name
   fn-meta
   fn-obj]
  (let [sym
        (-> fn-name name symbol)

        meta-new
        (assoc fn-meta :arglists
               '([db]
                 [db params]
                 [db params opt]))]

    (intern *ns*
            (with-meta sym meta-new)
            (-> fn-obj
                $wrap
                wrap-signature))))


(defn intern-sqlvec-function
  "
  Inject a sqlvec function produced by HugSQL
  into the current namespace.
  "
  [fn-name
   fn-meta
   fn-obj]
  (let [sym (-> fn-name name symbol)]
    (intern *ns* sym ($wrap fn-obj))))


(defn def-db-fns
  "
  Read and inject functions from a .sql file.
  Acts like `hugsql.core/def-db-fns` but overrides
  some internal steps.

  It automatically creates a PG2-related adapter
  and  assigns it to the newly created functions.

  Arguments:

  - `file` is either a string, or a resource, or a file
  object which is the source of .sql payload;

  - `defaults` is a map of arguments what will be passed
  to `pg.core/execute` on each call. See the PG2 readme file
  for more details;

  - `options` is map of HugSQL options. It's unlikely you
  will need to use it.

  Once this function is called, functions from the .sql
  file appear in the current namespace. Each function
  has the following signature:

  [db]
  [db params]
  [db params opt], where:

  - `db` is either a connection config map, or a connection,
  or a pool. If it was config map, the connection gets closed
  afterwards. When it's a pool, the connection is borrowed from
  it and then returned;

  - `params` is the standard HugSQL map of SQL parameters;

  - `opt`: pg2-specific arguments that are passed to the
  `pg.core/execute` function. They will override the defaults
  passed to the `def-db-fns` call.
  "

  ([file]
   (def-db-fns file nil nil))

  ([file defaults]
   (def-db-fns file defaults nil))

  ([file defaults options]

   (let [adapter
         (make-adapter defaults)

         defs
         (hugsql/map-of-db-fns
          file
          (assoc options :adapter adapter))]

     (doseq [[fn-name {fn-meta :meta
                       fn-obj :fn}]
             defs]
       (intern-function fn-name fn-meta fn-obj)))))


(defn def-sqlvec-fns

  ([file]
   (def-sqlvec-fns file nil))

  ([file options]

   (let [defs
         (hugsql/map-of-sqlvec-fns file options)]

     (doseq [[fn-name {fn-meta :meta
                       fn-obj :fn}]
             defs]
       (intern-sqlvec-function fn-name fn-meta fn-obj)))))
