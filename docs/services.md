# Services Tested With

This section brings real configuration samples for various services. Everyone is
welcome to share their settings for AWS, Azure, etc.

[neon.tech]: https://neon.tech/

## Neon.tech

[Neon.tech][neon.tech] requires SSL connection so always pass the `:use-ssl?`
flag:

~~~clojure
(def config
  {:host "ep-fancy-queen-XXXXX.eu-central-1.aws.neon.tech"
   :port 5432
   :user "test_owner"
   :password "<password>"
   :database "test"
   :use-ssl? true ;; always mandatory!
   })
~~~

## Supabase

In Supabase, SSL in optional and is off by default. You can enable it in the
dashboard:

![](/media/supabase.png)

When it's on, download the CA certificate file, make an instance of ssl-context
and pass it to the config as follows:

~~~clojure
(require '[pg.ssl :as ssl])

(def ssl-context
  (ssl/context "/Users/ivan/Downloads/prod-ca-2021.crt"))

(def config
  {:host "aws-0-eu-central-1.pooler.supabase.com"
   :port 6543
   :user "postgres.XXXX"
   :password "<password>"
   :database "postgres"
   :ssl-context ssl-context ;; mandatory when SSL is on
   })
~~~

See the ["SSL Setup"](/docs/ssl.md) section for details.
