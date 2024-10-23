(ns pg.pgvector-test
  (:import
   org.pg.error.PGError)
  (:require
   [pg.bb :refer [bb== ->bb]]
   [pg.type :as t]
   [clojure.test :refer [is deftest]]))

(set! *warn-on-reflection* true)


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


(deftest test-sparsevec-parse-bin
  (let [bb (->bb [0, 0, 0, 5, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 4, 63, -128, 0, 0, 64, 0, 0, 0, 64, 64, 0, 0])
        res (.decodeBin t/sparsevec bb nil)]
    (is (= "org.pg.type.SparseVector"
           (-> res class .getName)))

    (is (= "{1:1.0,3:2.0,5:3.0}/5"
           (str res)))

    (is (= 5 (count res)))

    (is (= {:dim 5
            :nnz 3
            :index {0 1.0 2 2.0 4 3.0}}
           @res))))

(deftest test-sparsevec-parse-txt

  (let [txt "{1:1.0,3:2.0,5:3.0}/5"
        res (.decodeTxt t/sparsevec txt nil)]
    (is (= "org.pg.type.SparseVector"
           (-> res class .getName)))
    (is (= "{1:1.0,3:2.0,5:3.0}/5"
           (str res)))
    (is (= 5 (count res)))
    (is (= {:dim 5
            :nnz 3
            :index {0 1.0 2 2.0 4 3.0}}
           @res))

    (is (= [1.0 0.0 2.0 0.0 3.0]
           (vec res)))

    (is (= 1.0 (nth res 0)))
    (is (= 0.0 (nth res 1)))
    (is (= 3.0 (last res)))
    (is (= 1.0 (first res)))

    (is (= "<SparseVector {1:1.0,3:2.0,5:3.0}/5>"
           (pr-str res))))

  (let [txt "   {   }   /   5   "
        res (.decodeTxt t/sparsevec txt nil)]
    (is (= "org.pg.type.SparseVector"
           (-> res class .getName)))

    (is (= "{}/5"
           (str res)))

    (is (= 5 (count res)))

    (is (= {:dim 5
            :nnz 0
            :index {}}
           @res))))


(deftest test-sparsevec-encode-bin
  (let [bb1 (->bb [0, 0, 0, 5, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 4, 63, -128, 0, 0, 64, 0, 0, 0, 64, 64, 0, 0])
        res (.decodeBin t/sparsevec bb1 nil)
        bb2 (.encodeBin t/sparsevec res nil)]

    (is (= (-> bb1 .array vec)
           (-> bb2 .array vec)))

    (.rewind bb2)

    (is (= {:nnz 3, :index {0 1.0, 4 3.0, 2 2.0}, :dim 5}
           @(.decodeBin t/sparsevec bb2 nil)))))

(deftest test-sparsevec-encode-txt-iter

  (is (= "{1:1.0,2:2.0,3:3.0,4:4.0,5:5.0}/5"
         (.encodeTxt t/sparsevec [1 2 3 4 5] nil)))

  (is (= "{}/8"
         (.encodeTxt t/sparsevec (repeat 8 0) nil)))

  (is (= "{1:-1.0,3:1.0,4:-1.0}/4"
         (.encodeTxt t/sparsevec (map inc [-2 -1 0 -2]) nil))))


;; TODO: sparsevec constructor

;; enc bin string
;; enc bin sv
;; enc bin iterable

;; enc txt string
;; enc txt sv
;; enc txt iterable
