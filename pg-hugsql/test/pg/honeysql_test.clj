(ns pg.honeysql-test
  (:require
   [clojure.string :as string]
   [hugsql.parameters :as p]
   [pg.hugsql :as pg.hug]
   [hugsql.core :as hugsql]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [clojure.java.io :as io]
   [pg.oid :as oid]
   [pg.pool :as pool]))


(def CONFIG
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})


(def adapter
  (pg.hug/adapter))


(def ^:dynamic *$* 0)


(defn $next []
  (set! *$* (inc *$*))
  (format "$%d" *$*))


(extend-type Object

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
          (string/join "," (for [_ coll] ($next)))]

      (into [placeholders] coll))))


(defn wrap$ [f]
  (fn [& args]
    (binding [*$* 0]
      (apply f args))))


(defn intern-func [fn-name fn-meta fn-obj]
  (let [sym
        (-> fn-name name symbol)]
    (intern *ns*
            (with-meta sym fn-meta)
            (wrap$ fn-obj))))


;; TODO: move to pg.hugsql
(defn load-funcs []
  (let [defs
        (hugsql/map-of-db-fns
         (io/file "test/pg/db.sql")
         {:adapter adapter})]

    (doseq [[fn-name {fn-meta :meta
                      fn-obj :fn}]
            defs]
      (intern-func fn-name fn-meta fn-obj))))


(deftest test-query-select-default
  (pg/with-connection [conn CONFIG]
    (let [result
          (try-select-jsonb conn
                            {:query "params"}
                            {:command "params"})]
      (is (= [{:json {:foo 42}}]
             result)))))


(deftest test-create-test-table-simple
  (pg/with-connection [conn CONFIG]

    (let [table
          (str (gensym "tmp"))

          result
          (create-test-table conn
                             {:table table})]

      (is (= {:command "CREATE TABLE"}
             result)))))


(deftest test-insert-into-table

  (pg/with-connection [conn CONFIG]

    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          result-insert
          (insert-into-table conn {:table table
                                   :title "Hello!"})

          result-query
          (select-from-table conn {:table table})]

      (is (= 1
             result-insert))

      (is (= 1
             result-query)))))


(deftest test-insert-into-table

  (pg/with-connection [conn CONFIG]

    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          result-insert
          (insert-into-table conn {:table table
                                   :title "Hello!"})

          result-query
          (select-from-table conn {:table table})

          result-get
          (get-by-id conn {:table table :id 1})

          result-miss
          (get-by-id conn {:table table :id 999})]

      (is (= 1
             result-insert))

      (is (= [{:title "Hello!", :id 1}]
             result-query))

      (is (= {:title "Hello!", :id 1}
             result-get))

      (is (= nil result-miss))

      )))

;; todo: execute custom params
;; insert returning
;; insert n
;; select where
;; insert json
