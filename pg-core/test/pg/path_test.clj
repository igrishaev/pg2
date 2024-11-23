(ns pg.point-test
  (:import
   (org.pg.type Path)
   org.pg.error.PGError)
  (:require
   [clojure.pprint :as pprint]
   [cheshire.core :as ch]
   [jsonista.core :as js]
   [pg.bb :refer [bb== ->bb]]
   [pg.oid :as oid]
   [pg.core :as pg]
   [pg.type :as t]
   [clojure.string :as str]
   [clojure.test :refer [is deftest testing]]))


(deftest test-props

  #_
  (let [p (Path/fromMap {:closed? true
                         :points [[1 2] [3 4] [5 6]]})]

    (is (= 1 p))
    )

  #_
  (let [p (Path/fromMap {:closed? false
                         :points [[1 2] [3 4] [5 6]]})]

    (is (= 1 p))
    )


  #_
  (let [p (t/path [[1 2] [3 4] [5 6]] true)])

  (let [p (Path/fromMap {
                         :points
                         (repeat 3 [1 2])

                         #_
                         [[1 2] [3 4] [5 6]]})]

    (is (= 1 p))
    )



  )
