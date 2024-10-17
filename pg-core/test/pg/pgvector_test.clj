(ns pg.pgvector-test
  (:import
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.processors :as p]
   [clojure.test :refer [is deftest]]))


(deftest test-vector-encode-txt
  (is (= "[1.0,2.0,3.0]"
         (.encodeTxt p/vector [1 2 3] nil)))
  (is (= "[2.0,3.0,4.0]"
         (.encodeTxt p/vector (map inc [1 2 3]) nil)))
  (is (= "[1.0,3.0,2.0]"
         (.encodeTxt p/vector #{1 2 3} nil)))
  (is (= "[1.0,2.0,3.0]"
         (.encodeTxt p/vector (list 1 2 3) nil)))
  (is (= "[1.2,2.3,3.4]"
         (.encodeTxt p/vector [1.2 2.3 3.4] nil)))

  (try
    (.encodeTxt p/vector [1.2 999999999999999999999999999999999999999999999999999999 3.4] nil)
    (is false)
    (catch PGError e
      (is (= "number 999999999999999999999999999999999999999999999999999999 is out of FLOAT4 range"
             (ex-message e)))))

  (try
    (.encodeTxt p/vector [1 nil 3] nil)
    (is false)
    (catch PGError e
      (is (= "item is not a number: NULL"
             (ex-message e))))))


(deftest test-vector-decode-txt
  (is (= [1.0 2.0 3.0]
         (.decodeTxt p/vector "[1, 2, 3]" nil)))
  (is (= (str [1.0 -2.3 -3.00009])
         (str (.decodeTxt p/vector "[  1.0  , -2.3  , -3.00009   ]" nil)))))


(deftest test-vector-encode-bin
  (is (= [0 3 0 0 63 -128 0 0 64 0 0 0 64 64 0 0]
         (vec (.array (.encodeBin p/vector [1 2 3] nil)))))
  (is (= [0 1 0 0 63 -128 0 0]
         (vec (.array (.encodeBin p/vector [1] nil)))))
  (is (= [0 0 0 0]
         (vec (.array (.encodeBin p/vector [] nil)))))
  (is (= [0 3 0 0 64 0 0 0 64 64 0 0 64 -128 0 0]
         (vec (.array (.encodeBin p/vector (map inc [1 2 3]) nil)))))
  (try
    (.encodeBin p/vector [1.2 999999999999999999999999999999999999999999999999999999 3.4] nil)
    (is false)
    (catch PGError e
      (is (= "number 999999999999999999999999999999999999999999999999999999 is out of FLOAT4 range"
             (ex-message e)))))
  (try
    (.encodeBin p/vector [1 nil 3] nil)
    (is false)
    (catch PGError e
      (is (= "item is not a number: NULL"
             (ex-message e))))))


(deftest test-vector-decode-bin
  (let [bb (->bb [0 3 0 0 63 -128 0 0 64 0 0 0 64 64 0 0])]
    (is (= [1.0 2.0 3.0]
           (.decodeBin p/vector bb nil)))))
