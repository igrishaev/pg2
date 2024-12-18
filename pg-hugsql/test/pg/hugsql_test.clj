(ns pg.hugsql-test
  (:require
   [pg.hugsql :as hugsql]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.oid :as oid]
   [pg.pool :as pool]))


(def CONFIG
  {:host "127.0.0.1"
   :port 10150
   :user "test"
   :password "test"
   :database "test"})


(hugsql/def-db-fns (io/file "test/pg/test.sql"))

(hugsql/def-sqlvec-fns (io/file "test/pg/test.sql"))

#_
(hugsql/def-sqlvec-fns (io/file "test/pg/test.sql") {:fn-suffix "-hello"})


(deftest test-query-select-default
  (pg/with-connection [conn CONFIG]
    (let [result
          (try-select-jsonb conn)]
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

      (is (= nil result-miss)))))


(deftest test-select-json-from-param
  (pg/with-connection [conn CONFIG]
    (let [res
          (select-json-from-param conn {:json {:foo {:bar 42}}})]
      (is (= {:json {:foo {:bar 42}}} res)))))


(deftest test-insert-into-table-ret

  (pg/with-connection [conn CONFIG]
    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          result-insert
          (insert-into-table-ret conn {:table table
                                       :title "Hello!"})]

      (is (= [{:title "Hello!", :id 1}]
             result-insert)))))


(deftest test-select-value-list

  (pg/with-connection [conn CONFIG]
    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          _
          (insert-into-table conn {:table table
                                   :title "aaa"})

          _
          (insert-into-table conn {:table table
                                   :title "bbb"})

          _
          (insert-into-table conn {:table table
                                   :title "ccc"})

          res
          (select-value-list conn {:table table
                                   :ids [1 3]})]

      (is (= [{:title "aaa", :id 1} {:title "ccc", :id 3}]
             res)))))


(deftest test-select-tuple-param

  (pg/with-connection [conn CONFIG]
    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          _
          (insert-into-table conn {:table table
                                   :title "aaa"})

          _
          (insert-into-table conn {:table table
                                   :title "bbb"})

          res
          (select-tuple-param conn {:table table
                                    :pair [2 "bbb"]})]

      (is (= [{:title "bbb", :id 2}]
             res)))))

(deftest test-insert-tuple-list

  (pg/with-connection [conn CONFIG]
    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          res
          (insert-tuple-list conn {:table table
                                   :rows
                                   [[1 "aaa"]
                                    [2 "bbb"]
                                    [3 "ccc"]]})]

      (is (= [{:title "aaa", :id 1}
              {:title "bbb", :id 2}
              {:title "ccc", :id 3}]
             res)))))

(deftest test-select-identifiers-list

  (pg/with-connection [conn CONFIG]
    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          _
          (insert-tuple-list conn {:table table
                                   :rows
                                   [[1 "aaa"]
                                    [2 "bbb"]
                                    [3 "ccc"]]})

          res
          (select-identifiers-list conn {:table table
                                         :fields ["title"]
                                         :order-by "id"})]

      (is (= [{:title "aaa"}
              {:title "bbb"}
              {:title "ccc"}]
             res)))))


(deftest test-pass-config
  (let [result
        (try-select-jsonb CONFIG)]
    (is (= [{:json {:foo 42}}]
           result))))


(deftest test-pass-pool
  (pool/with-pool [pool CONFIG]
    (let [result
          (try-select-jsonb pool)]
      (is (= [{:json {:foo 42}}]
             result)))))


(deftest test-adapter-override-defaults
  (pg/with-connection [conn CONFIG]
    (let [result
          (try-select-jsonb conn
                            nil
                            {:fn-key str/upper-case})]
      (is (= [{"JSON" {:foo 42}}]
             result)))))


(deftest test-func-meta
  (let [result
        (-> try-select-jsonb var meta)]
    (is (= {:doc ""
            :command :?
            :result :*
            :file "test/pg/test.sql"
            :line 2
            :arglists '([db] [db params] [db params opt])
            :name 'try-select-jsonb
            :ns (the-ns 'pg.hugsql-test)}
           result))))


(deftest test-transaction

  (pg/with-connection [conn CONFIG]

    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          result-inner
          (pg/with-tx [conn {:rollback? true}]
            (insert-into-table conn {:table table
                                     :title "AAA"})
            (insert-into-table conn {:table table
                                     :title "BBB"})
            (select-from-table conn {:table table}))

          result-outter
          (select-from-table conn {:table table})]

      (is (= [{:title "AAA", :id 1}
              {:title "BBB", :id 2}]
             result-inner))
      (is (= [] result-outter)))))


(deftest test-sqlvec-ok

  (let [result
        (try-select-jsonb-sqlvec)]
    (is (= ["select '{\"foo\": 42}'::jsonb as json"]
           result)))

  (is (= {:name 'try-select-jsonb-sqlvec}
         (-> try-select-jsonb-sqlvec
             var
             meta
             (dissoc :ns)))))


(deftest test-sqlvec-with-params

  (let [result
        (insert-into-table-sqlvec {:table "table"
                                   :title "hello"})]
    (is (= ["insert into table (title) values ($1);" "hello"]
           result)))

  (let [result
        (select-value-list-sqlvec {:table "table"
                                   :ids [1 2 3]})]
    (is (= ["select * from table where id in ($1,$2,$3) order by id;" 1 2 3]
           result))))


(deftest test-sqlvec-missing-params
  (try
    (insert-into-table-sqlvec {:table "table"})
    (is false)
    (catch Exception e
      (is (= "Parameter Mismatch: :title parameter data not found."
             (ex-message e))))))


(deftest test-two-arguments
  (let [result
        (insert-into-table-sqlvec {:table "table"
                                   :title "hello"}
                                  {:some :options})]
    (is (= ["insert into table (title) values ($1);" "hello"]
           result))))


#_
(deftest test-custom-suffix
  (let [result
        (insert-into-table-hello {:table "table"
                                  :title "hello"}
                                 {:some :options})]
    (is (= ["insert into table (title) values ($1);" "hello"]
           result))))


;;
;; #25: nested snippets
;;

(deftest test-sqlvec-nested-snippet
  (let [result
        (snip-query-sqlvec {:select (select-snip {:foo 1 :bar "2"})
                            :kek 3
                            :lol "4"})]
    (is (= ["select $1, $2 where id = $1 and email = $2" 1 "2" 3 "4"]
           result))))

(deftest test-sqlvec-nested-query
  (pg/with-connection [conn CONFIG]
    (let [table
          (str (gensym "tmp"))

          _
          (create-test-table conn
                             {:table table})

          _
          (insert-into-table conn {:table table
                                   :title "aaa"})

          _
          (insert-into-table conn {:table table
                                   :title "bbb"})

          result
          (select-with-snippets conn
                                {:select (select-cols {:cols ["id" "title"]})
                                 :table table
                                 :id -999
                                 :filter-id (filter-by-id {:id 1})
                                 :filter-title (filter-by-title {:title "aaa"})
                                 :order-by "id"})]

      (is (= [{:title "aaa", :id 1}]
             result)))))


#_
(select-with-snippets-sqlvec
 {:select (select-cols {:cols ["id" "title"]})
  :table "aaa"
  :id -999
  :filter-id (filter-by-id {:id 1})
  :filter-title (filter-by-title {:title "aaa"})
  :order-by "id"})


#_
(-> (select-with-snippets-sqlvec
     {:select (select-cols {:cols ["id" "title"]})
      :table "aaa"
      :id -999
      :filter-id (filter-by-id {:id 1})
      :filter-title (filter-by-title {:title "aaa"})
      :order-by "id"})
    first
    println
    )
