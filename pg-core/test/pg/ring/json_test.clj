(ns pg.ring.json-test
  (:import
   java.io.InputStream
   java.io.ByteArrayOutputStream
   java.io.ByteArrayInputStream)
  (:require
   [clojure.test :refer [deftest is]]
   [pg.json :as json]
   [pg.ring.json :refer [wrap-json-response
                         wrap-json-request]]))


(deftest test-wrap-response-coll

  (let [handler
        (-> (fn [request]
              {:status 200
               :body {:foo 42}})
            wrap-json-response)

        response
        (handler {})]

    (is (= {:status 200,
            :body "{\"foo\":42}"
            :headers {"content-type" "application/json; encoding=utf-8"}}
           response))))


(deftest test-wrap-response-not-coll

  (let [handler
        (-> (fn [request]
              {:status 200
               :body "hello"
               :headers {"content-type" "text/plain"}})
            wrap-json-response)

        response
        (handler {})]

    (is (= {:status 200
            :body "hello"
            :headers {"content-type" "text/plain"}}
           response))))


(deftest test-wrap-request-ok

  (let [capture!
        (atom nil)

        handler
        (-> (fn [request]
              (reset! capture! request)
              {:status 200
               :body "OK"})
            wrap-json-request)

        body
        (new ByteArrayInputStream
             (.getBytes "[1, 2, 3]" "UTF-8"))

        request
        {:body body
         :headers {"content-type" "application/json"}}

        response
        (handler request)

        request'
        @capture!]

    (is (instance? InputStream (:body request')))

    (is (= {:headers {"content-type" "application/json"}
            :json [1 2 3]}
           (dissoc request' :body)))))


;; not a json
;; malformerd
;; custom options
;; mixed
