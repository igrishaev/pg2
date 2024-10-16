(ns pg.util-test
  (:require
   [pg.core :as pg]
   [clojure.test :refer [deftest is testing]])
  (:import
   org.pg.error.PGError))


(deftest test-dunno
  (is (= 1 1)))
