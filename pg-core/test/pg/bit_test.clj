(ns pg.bit-test
  (:import
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.processors :as p]
   [pg.oid :as oid]
   [pg.core :as pg]
   [clojure.test :refer [is deftest]]))


(deftest test-bit-encode-bin
  (let [bb (pg/encode-bin "00000100" oid/bit)]
    (is (= 1
           (-> bb .array vec)))
)

  )
