(ns pg.ring.json
  "
  Like ring.middleware.json but faster because uses
  internal JSON engine instead of Cheshire.
  "
  (:require
   [pg.json :as json]))


(def ^:const CT_JSON
  "application/json; encoding=utf-8")


(defn wrap-json-response [handler]
  (fn wrapper [request]
    (let [response (handler request)]
      (if (-> response :body coll?)
        (-> response
            (update :body pg.json/write-string)
            (assoc-in [:headers "content-type"] CT_JSON))
        response))))


(defn json-request? [request]
  (when-let [content-type
             (get-in request [:headers "content-type"])]
    (re-find #"^application/(.+\+)?json" content-type)))


(def ^:const JSON_ERR ::__error)


(def RESP-JSON-MALFORMED
  {:status 400
   :headers {"Content-Type" "text/plain"}
   :body  "Malformed JSON in request body."})


(defn wrap-json-request
  ([handler]
   (wrap-json-request handler nil))

  ([handler {:keys [slot malformed-response]
             :or {slot :json
                  malformed-response RESP-JSON-MALFORMED}}]
   (fn wrapper [request]
     (if-not (json-request? request)
       (handler request)

       (let [{:keys [body]}
             request

             json
             (try
               (json/read-stream body)
               (catch Throwable e
                 JSON_ERR))]

         (if (identical? json JSON_ERR)
           malformed-response
           (-> request
               (assoc slot json)
               (handler))))))))
