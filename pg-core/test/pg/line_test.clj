(ns pg.line-test
  (:import
   (org.pg.type Line)
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.test :refer [is deftest testing]]))

(deftest test-props
  (let [l (t/line 1 2 3)]
    (is (instance? Line l))

    (is (= "{1.0,2.0,3.0}" (str l)))
    (is (= "<Line {1.0,2.0,3.0}>" (pr-str l)))
    (is (= {:a 1.0 :b 2.0 :c 3.0} @l))

    (is (= 1.0 (:a l)))
    (is (= 2.0 (:b l)))
    (is (= 3.0 (:c l)))

    ;; (is (= 1.0 (nth p 0)))
    ;; (is (= 2.0 (nth p 1)))

    ;; (is (= 2.0 (get p :y)))

    ;; (is (nil? (get p "abc")))

    ;; (is (= 2 (count p)))
    ;; (is (= [1.0 2.0] (vec p)))

    ;; (is (= ::miss (nth p 99 ::miss)))

    #_
    (try
      (nth p 2)
      (is false)
      (catch IndexOutOfBoundsException e
        (is true)))))
