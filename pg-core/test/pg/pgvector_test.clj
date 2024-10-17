(ns pg.pgvector-test
  (:import
   org.pg.error.PGError
   org.pg.type.processor.IProcessor
   org.pg.type.processor.Processors)
  (:require
   [clojure.test :refer [is deftest]]))


(def ^IProcessor P
  Processors/pgVector)

(deftest test-encode-txt
  (is (= "[1.0,2.0,3.0]"
         (.encodeTxt P [1 2 3] nil)))
  (is (= "[2.0,3.0,4.0]"
         (.encodeTxt P (map inc [1 2 3]) nil)))
  (is (= "[1.0,3.0,2.0]"
         (.encodeTxt P #{1 2 3} nil)))
  (is (= "[1.0,2.0,3.0]"
         (.encodeTxt P (list 1 2 3) nil)))
  (is (= "[1.2,2.3,3.4]"
         (.encodeTxt P [1.2 2.3 3.4] nil)))

  (try
    (.encodeTxt P [1.2 999999999999999999999999999999999999999999999999999999 3.4] nil)
    (is false)
    (catch PGError e
      (is (= "number 999999999999999999999999999999999999999999999999999999 is out of FLOAT4 range"
             (ex-message e)))))

  (try
    (.encodeTxt P [1 nil 3] nil)
    (is false)
    (catch PGError e
      (is (= 1
             (ex-message e)))))



  )
