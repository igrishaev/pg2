(ns pg.polygon-test
  (:import
   (org.pg.type Polygon)
   org.pg.error.PGError)
  (:require
   [cheshire.core :as ch]
   [jsonista.core :as js]
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.test :refer [is deftest testing]]))


(deftest test-props
  (let [p (t/polygon [[1 2] [3 4]])]
    (is (t/polygon? p))))
