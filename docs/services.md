# Services Tested With

~~~clojure
(def config
  {:host "ep-fancy-queen-XXXXX.eu-central-1.aws.neon.tech"
   :port 5432
   :user "test_owner"
   :password "<password>"
   :database "test"
   :use-ssl? true ;; mandatory!
   })
~~~

~~~clojure
(pg/with-conn [conn config]
  (pg/query conn "select 1"))
~~~

~~~clojure
(require '[pg.ssl :as ssl])

(def ssl-context
  (ssl/context "/Users/ivan/Downloads/prod-ca-2021.crt"))
~~~

~~~clojure
(def config
  {:host "aws-0-eu-central-1.pooler.supabase.com"
   :port 6543
   :user "postgres.XXXX"
   :password "<password>"
   :database "postgres"
   :ssl-context ssl-context ;; mandatory!
   })
~~~
