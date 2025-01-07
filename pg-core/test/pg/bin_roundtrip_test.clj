(ns pg.bin-roundtrip-test
  (:require
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.results :as results]
   [pg.generators :as pg-gen]
   [pg.core :as pg]
   [pg.oid :as oid])
  (:import java.util.Arrays
           [java.time
            OffsetDateTime
            ZoneOffset]))

(defn should=
  [a b]
  (reify results/Result
    (pass? [_] (= a b))
    (result-data [_]
      {:a a
       :b b})))

(defspec round-trip-numeric
  (prop/for-all [val1 (gen/one-of
                       [gen/int
                        gen/size-bounded-bigint
                        pg-gen/bigdecimal])]
                (let [buf (pg/encode-bin val1 oid/numeric)
                      val2 (pg/decode-bin buf oid/numeric)]
                  (should= (bigdec val1) val2))))

(defspec round-trip-numeric-array-dim-1
  (prop/for-all [val1 (gen/vector pg-gen/bigdecimal)]
                (let [buf (pg/encode-bin val1 oid/_numeric)
                      val2 (pg/decode-bin buf oid/_numeric)]
                  (should= val1 val2))))

(defspec round-trip-numeric-array-dim-2
  (prop/for-all [val1 (pg-gen/vector-2d (gen/frequency [[9 pg-gen/bigdecimal]
                                                        [1 (gen/return nil)]]))]
                (let [buf (pg/encode-bin val1 oid/_numeric)
                      val2 (pg/decode-bin buf oid/_numeric)]
                  (should= val1 val2))))

(defspec round-trip-text
  (prop/for-all [val1 gen/string
                 oid (gen/elements [oid/text oid/varchar])]
                (let [val2 (pg/decode-bin (pg/encode-bin val1 oid) oid)]
                  (should= val1 val2))))

(defspec round-trip-int4
  (prop/for-all [val1 gen/small-integer]
                (let [val2 (pg/decode-bin (pg/encode-bin val1 oid/int4) oid/int4)]
                  (should= val1 val2))))

(defspec round-trip-int8
  (prop/for-all [val1 (gen/one-of [gen/small-integer gen/large-integer])]
                (let [val2 (pg/decode-bin (pg/encode-bin val1) oid/int8)]
                  (should= val1 val2))))

(defspec round-trip-float4
  (prop/for-all [val1 (gen/double* {:NaN? false})]
                (let [val2 (pg/decode-bin (pg/encode-bin val1) oid/float8)]
                  (should= val1 val2))))

(defspec round-trip-float8
  (prop/for-all [val1 (gen/fmap #(float (.floatValue %)) (gen/double* {:NaN? false}))]
                (let [val2 (pg/decode-bin (pg/encode-bin val1) oid/float4)]
                  (should= val1 val2))))

(defspec round-trip-jsonb 50
  (prop/for-all [val1 pg-gen/jsonable-collection]
                (let [val2 (pg/decode-bin (pg/encode-bin val1 oid/jsonb) oid/jsonb)]
                  (should= val1 val2))))

(defspec round-trip-timestamptz
  (prop/for-all [val1 (gen/fmap #(OffsetDateTime/ofInstant % ZoneOffset/UTC) pg-gen/instant)]
                (let [val2 (pg/decode-bin (pg/encode-bin val1 oid/timestamptz) oid/timestamptz)]
                  (should= val1 val2))))

(defspec round-trip-date
  (prop/for-all [val1 pg-gen/local-date]
                (let [val2 (pg/decode-bin (pg/encode-bin val1) oid/date)]
                  (should= val1 val2))))

(defspec round-trip-bytea
  (prop/for-all [val1 (gen/not-empty gen/bytes)]
                (let [val2 (pg/decode-bin (pg/encode-bin val1) oid/bytea)]
                  (Arrays/equals val1 val2))))
