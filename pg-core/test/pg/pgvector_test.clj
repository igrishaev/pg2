(ns pg.pgvector-test
  (:import
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.type :as t]
   [clojure.test :refer [is deftest]]))


(deftest test-vector-encode-txt
  (is (= "[1.0,2.0,3.0]"
         (.encodeTxt t/vector [1 2 3] nil)))
  (is (= "[2.0,3.0,4.0]"
         (.encodeTxt t/vector (map inc [1 2 3]) nil)))
  (is (= "[1.0,3.0,2.0]"
         (.encodeTxt t/vector #{1 2 3} nil)))
  (is (= "[1.0,2.0,3.0]"
         (.encodeTxt t/vector (list 1 2 3) nil)))
  (is (= "[1.2,2.3,3.4]"
         (.encodeTxt t/vector [1.2 2.3 3.4] nil)))

  (try
    (.encodeTxt t/vector [1.2 999999999999999999999999999999999999999999999999999999 3.4] nil)
    (is false)
    (catch PGError e
      (is (= "number 999999999999999999999999999999999999999999999999999999 is out of FLOAT4 range"
             (ex-message e)))))

  (try
    (.encodeTxt t/vector [1 nil 3] nil)
    (is false)
    (catch PGError e
      (is (= "item is not a number: NULL"
             (ex-message e))))))


(deftest test-vector-decode-txt
  (is (= [1.0 2.0 3.0]
         (.decodeTxt t/vector "[1, 2, 3]" nil)))
  (is (= (str [1.0 -2.3 -3.00009])
         (str (.decodeTxt t/vector "[  1.0  , -2.3  , -3.00009   ]" nil)))))


(deftest test-vector-encode-bin
  (is (= [0 3 0 0 63 -128 0 0 64 0 0 0 64 64 0 0]
         (vec (.array (.encodeBin t/vector [1 2 3] nil)))))
  (is (= [0 1 0 0 63 -128 0 0]
         (vec (.array (.encodeBin t/vector [1] nil)))))
  (is (= [0 0 0 0]
         (vec (.array (.encodeBin t/vector [] nil)))))
  (is (= [0 3 0 0 64 0 0 0 64 64 0 0 64 -128 0 0]
         (vec (.array (.encodeBin t/vector (map inc [1 2 3]) nil)))))
  (try
    (.encodeBin t/vector [1.2 999999999999999999999999999999999999999999999999999999 3.4] nil)
    (is false)
    (catch PGError e
      (is (= "number 999999999999999999999999999999999999999999999999999999 is out of FLOAT4 range"
             (ex-message e)))))
  (try
    (.encodeBin t/vector [1 nil 3] nil)
    (is false)
    (catch PGError e
      (is (= "item is not a number: NULL"
             (ex-message e))))))


(deftest test-vector-decode-bin
  (let [bb (->bb [0 3 0 0 63 -128 0 0 64 0 0 0 64 64 0 0])]
    (is (= [1.0 2.0 3.0]
           (.decodeBin t/vector bb nil)))))


(deftest test-sparsevec-ok
  (let [bb (->bb [0, 0, 0, 5, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 4, 63, -128, 0, 0, 64, 0, 0, 0, 64, 64, 0, 0])]
    (is (= [1.0 0 2.0 0 3.0]
           (.decodeBin t/sparsevec bb nil)))
    )


  )
