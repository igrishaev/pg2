(ns pg.hugsql
  "
  HugSQL adapter for PG2. Hacks, hacks, hacks.
  "
  (:import
   java.util.function.Function)
  (:require
   [clojure.string :as string]
   [hugsql.adapter :as adapter]
   [hugsql.core :as hugsql]
   [hugsql.parameters :as p :refer [deep-get-vec]]
   [pg.core :as pg]))


;; A special stub to not mix ? parameters with
;; question marks in string literals, comments,
;; operators, etc.
(def ^:const PARAM "__PaRaM__")
(def ^:const $PARAM (str "$" PARAM))
(def ^:const PARAM_RE (re-pattern PARAM))


(defn remap-$-params
  "
  Replace all parameter stubs with $1, $2, etc.
  "
  [^String sql]
  (.replaceAll (re-matcher PARAM_RE sql)
               (let [counter! (atom 0)]
                 (reify Function
                   (apply [this x]
                     (swap! counter! inc)
                     (str @counter!))))))


(extend-type Object

  ;; Here and below: instead of ? for a parameter,
  ;; put a recognizable figure like __PaRaM__ for
  ;; further processing.

  p/ValueParam
  (value-param [param data options]
    [$PARAM
     (get-in data (deep-get-vec (:name param)))])

  p/ValueParamList
  (value-param-list [param data options]
    (let [coll (get-in data (deep-get-vec (:name param)))]
      (apply vector
             (string/join ","  (repeat (count coll) $PARAM))
             coll))))


(deftype PG2Adapter [defaults]

  adapter/HugsqlAdapter

  (execute [this db sqlvec options]

    (let [[sql & params]
          sqlvec

          sql ;; patch SQL with __PaRaM__ figures
          (remap-$-params sql)

          {opt :pg}
          options

          opt-full
          (-> defaults
              (merge opt)
              (assoc :params params))]

      (pg/with-conn [conn db]
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


(defn wrap-$-params
  "
  Post-correct SQL vector: replace __PaRaM__ figures
  with dollar parameters.
  "
  [-f]
  (fn [& args]
    (let [sqlvec (apply -f args)]
      (update sqlvec 0 remap-$-params))))


(defn wrap-signature
  "
  Slightly correct the signature of a function
  produced by HugSQL. Namely, pass `pg.core/execute`
  parameters through a dedicated key to prevent
  merging them with something else.
  "
  [-f]
  (fn
    ([db]
     (-f db))
    ([db params]
     (-f db params))
    ([db params opt]
     (-f db params {:pg opt}))))


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
                wrap-signature))))


(defn intern-sqlvec-function
  "
  Inject a sqlvec function produced by HugSQL
  into the current namespace.
  "
  [fn-name
   fn-meta
   fn-obj]
  (let [sym
        (-> fn-name name symbol)

        ;; Trick: do not wrap snippets because
        ;; their correction happens not here
        ;; but at the final step.
        {:keys [snip?]}
        fn-meta]

    (intern *ns*
            sym
            (if snip?
              fn-obj
              (-> fn-obj
                  wrap-$-params)))))


(defn def-db-fns
  "
  Read and inject functions from a .sql file.
  Acts like `hugsql.core/def-db-fns` but overrides
  some internal steps.

  It automatically creates a PG2-related adapter
  and  assigns it to the newly created functions.

  Arguments:

  - `file` is either a string, or a resource, or a file
  object which is the source of a .sql payload;

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
  "
  Read and inject sqlvec-functions from a .sql file.
  These functions don't interact with a database
  but produce pure SQL vectors. Useful for debugging
  SQL you wrote, and for unit tests.

  Acts like `hugsql.core/def-db-fns` but with some
  internal steps.

  - `file` is either a string, or a resource, or a file
  object which is the source of a .sql payload;

  - `options` is a map of hugsql parameters. The most
  interesting is `:fn-suffix` to override the default
  '-sqlvec' ending with something else.
  "
  ([file]
   (def-sqlvec-fns file nil))

  ([file options]
   (let [defs
         (hugsql/map-of-sqlvec-fns file options)]
     (doseq [[fn-name {fn-meta :meta
                       fn-obj :fn}]
             defs]
       (intern-sqlvec-function fn-name fn-meta fn-obj)))))
