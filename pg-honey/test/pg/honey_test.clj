(ns pg.honey-test
  (:require
   [clojure.test :refer [is deftest]]
   [pg.honey :as pgh]))


(deftest test-sql-format-ok

  (let [result
        (pgh/format {:select [:foo :bar :baz]
                     :from [[:users :u]]
                     :where [:and
                             [:= :name "Ivan"]
                             [:= :age 37]]})]

    (is (= ["SELECT foo, bar, baz FROM users AS u WHERE (name = $1) AND (age = $2)"
            "Ivan" 37]
           result))))


(deftest test-sql-format-params

  (let [result
        (pgh/format
         {:select [:foo :bar :baz]
          :from [[:users :u]]
          :where [:and
                  [:= :name [:param :name]]
                  [:= :age [:param :age]]]}
         {:pretty true
          :params {:name "Ivan"
                   :age 37}})]

    (is (= ["
SELECT foo, bar, baz
FROM users AS u
WHERE (name = $1) AND (age = $2)
"
            "Ivan"
            37]
           result))))
