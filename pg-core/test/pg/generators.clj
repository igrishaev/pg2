(ns pg.generators
  (:require [clojure.test.check.generators :as gen])
  (:import [java.math BigDecimal]
           [java.time Instant
                      OffsetDateTime
                      LocalDate
                      ZoneOffset]))

(def bigdecimal
  (gen/fmap (fn [[a b]]
              (BigDecimal. (biginteger a) b))
            (gen/tuple gen/size-bounded-bigint gen/nat)))

(defn vector-2d
  [generator & {:keys [min-sub-size max-sub-size min-size max-size]
                :or {min-sub-size 0
                     max-sub-size 20
                     min-size 0
                     max-size 20}}]
  (assert (gen/generator? generator) "First arg must be a generator")
  (gen/bind (gen/choose min-sub-size max-sub-size)
            (fn [n]
              (gen/vector (gen/vector generator n) min-size max-size))))

(def jsonable-collection
  (gen/vector
   (gen/recursive-gen
    (fn [inner]
      (gen/one-of [(gen/vector inner)
                   (gen/map gen/keyword inner)]))
    (gen/one-of [(gen/return nil)
                 gen/boolean
                 gen/string
                 gen/small-integer
                 (gen/double* {:NaN? false :infinite? false})]))))

(def inst-ms-min (.toEpochMilli (Instant/parse "1800-01-01T00:00:00.000Z")))
(def inst-ms-max (.toEpochMilli (Instant/parse "2499-12-31T23:59:59.999Z")))

(def instant (gen/fmap (fn [ms] (Instant/ofEpochMilli ms))
                       (gen/choose inst-ms-min inst-ms-max)))

(def offset-zones
  (gen/elements [ZoneOffset/UTC
                 (ZoneOffset/ofHours -4)
                 (ZoneOffset/ofHoursMinutes 6 30)
                 (ZoneOffset/ofHours 1)
                 (ZoneOffset/ofHours 13)]))

(def offset-date-time
  (gen/fmap (fn [[inst zone]]
              (OffsetDateTime/ofInstant inst zone))
            (gen/tuple instant offset-zones)))

(def local-date
  (gen/fmap (fn [inst] (LocalDate/ofInstant inst ZoneOffset/UTC)) instant))
