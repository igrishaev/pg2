(ns pg.ring.json
  "
  Like `ring.middleware.json` but faster because uses
  internal JSON engine instead of Cheshire.
  "
  (:require
   [pg.json :as json]))


(def ^:const CT_JSON
  "application/json; encoding=utf-8")


(defn wrap-json-response
  "
  If the :body of the response is a collection, JSON-encode
  it and add a corresponding HTTP header.
  "
  ([handler]
   (wrap-json-response handler nil))
  ([handler _]
   (fn wrapper [request]
     (let [response (handler request)]
       (if (-> response :body coll?)
         (-> response
             (update :body pg.json/write-string)
             (assoc-in [:headers "content-type"] CT_JSON))
         response)))))


(defn json-request?
  "
  True if it was a request with JSON payload.
  "
  [request]
  (when-let [content-type
             (get-in request [:headers "content-type"])]
    (re-find #"^application/(.+\+)?json" content-type)))


(def ^:const JSON_ERR ::__error)


(def RESP-JSON-MALFORMED
  {:status 400
   :headers {"Content-Type" "text/plain"}
   :body  "Malformed JSON in request body."})


(defn wrap-json-request
  "
  If it was a JSON request, parse the body and assoc the data
  into a dedicated field. We *do not* overwrite the :body field
  because it's still might be needed to calculate a checksum,
  for example.

  Suports the following options:
  - :slot -- the field where the parsed JSON data gets associated;
  - :malformed-response -- a response map which gets returned
    when we could not parse the payload. Default is 400 with
    a plain text error message.
  "
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


(defn wrap-json
  "
  Two in one: wrap a handler with wrap-json-request
  and wrap-json-response. All the optional parameters
  are supported.
  "
  ([handler]
   (wrap-json handler nil))
  ([handler options]
   (-> handler
       (wrap-json-request options)
       (wrap-json-response options))))
