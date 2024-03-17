(ns pg.ring.json-test
  (:import
   java.io.ByteArrayInputStream
   java.io.ByteArrayOutputStream
   java.io.InputStream)
  (:require
   [clojure.test :refer [deftest is]]
   [jsonista.core :as j]
   [pg.json :as json]
   [pg.ring.json :refer [wrap-json
                         wrap-json-response
                         wrap-json-request]]))


(defn reverse-string [s]
  (apply str (reverse s)))


(def custom-mapper
  (j/object-mapper
   {:encode-key-fn (comp reverse-string name)
    :decode-key-fn (comp keyword reverse-string)}))


(deftest test-wrap-response-coll

  (let [opt
        {:object-mapper custom-mapper}

        handler
        (-> (fn [request]
              {:status 200
               :body {:foo 42}})
            (wrap-json-response opt))

        response
        (handler {})]

    (is (= {:status 200,
            :body "{\"oof\":42}"
            :headers {"content-type"
                      "application/json; encoding=utf-8"}}
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

  (let [opt
        {:object-mapper custom-mapper}

        capture!
        (atom nil)

        handler
        (-> (fn [request]
              (reset! capture! request)
              {:status 200
               :body "OK"})
            (wrap-json-request opt))

        body
        (new ByteArrayInputStream
             (.getBytes "{\"foo\": 42}" "UTF-8"))

        request
        {:body body
         :headers {"content-type" "application/json"}}

        response
        (handler request)

        request'
        @capture!]

    (is (instance? InputStream (:body request')))

    (is (= {:headers {"content-type" "application/json"}
            :json {:oof 42}}
           (dissoc request' :body)))))


(deftest test-wrap-request-ok-slot

  (let [capture!
        (atom nil)

        handler
        (-> (fn [request]
              (reset! capture! request)
              {:status 200
               :body "OK"})
            (wrap-json-request {:slot :DATA}))

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
            :DATA [1 2 3]}
           (dissoc request' :body)))))


(deftest test-wrap-request-not-json

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
         :headers {"content-type" "text/plain"}}

        response
        (handler request)

        request'
        @capture!]

    (is (instance? InputStream (:body request')))

    (is (= {:headers {"content-type" "text/plain"}}
           (dissoc request' :body)))))


(deftest test-wrap-request-malformed

  (let [capture!
        (atom nil)

        handler
        (-> (fn [request]
              (reset! capture! request)
              {:status 200
               :body "OK"})
            (wrap-json-request {:malformed-response
                                {:status 666
                                 :body "error"}}))

        body
        (new ByteArrayInputStream
             (.getBytes "[1, 2, ?dunno/lol?, 3]" "UTF-8"))

        request
        {:body body
         :headers {"content-type" "application/json"}}

        response
        (handler request)]

    (is (nil? @capture!))
    (is (= {:status 666
            :body "error"} response))))


(deftest test-wrap-mixed

  (let [capture!
        (atom nil)

        handler
        (-> (fn [request]
              (reset! capture! request)
              {:status 200
               :body {:foo "kek"}})
            (wrap-json {:slot :foobar
                        :object-mapper custom-mapper}))

        body
        (new ByteArrayInputStream
             (.getBytes "{\"foo\": 123}" "UTF-8"))

        request
        {:body body
         :headers {"content-type" "application/json"}}

        response
        (handler request)]

    (is (= {:headers {"content-type" "application/json"}
            :foobar {:oof 123}}
           (dissoc @capture! :body)))

    (is (= {:status 200
            :body "{\"oof\":\"kek\"}"
            :headers {"content-type" "application/json; encoding=utf-8"}}
           response))))
