# Services Tested With

This section brings real configuration samples for various services. Everyone is
welcome to share their settings for AWS, Azure, etc.

## AWS RDS (Postgres Native)

When reaching an RDS database from an internal resource (EC2, Lambda, etc),
there is no need to specify SSL. But if you want to reach the database from the
outer world (e.g. your office), some preparation is required.

First, make your database publicly available. Select your database, click
"Modify". Scroll down to the "Connectivity" section, and expand the "Additional
configuration" widget. Mark the "Publicly available" checkbox:

![](/media/aws1.png)

Second, open a security group and allow incoming TCP traffic on port 5432:

![](/media/aws2.png)

Now that you have done it, connect to the database as follows:

~~~clojure
(def config
  {:host "database-1.cxyw0khwga3x.us-east-1.rds.amazonaws.com"
   :port 5432
   :user "postgres"
   :password "......"
   :database "postgres"
   :ssl? true}) ;; SSL is mandatory!

(with-conn [conn config]
  (query conn "select 1"))
~~~

When reaching the database from EC2 or Lambda, you can skip the `:ssl?` flag.

## AWS Aurora (Postgres compatible)

The same as above. Make you database publicly available and setup a
corresponding security group to allow incoming TCP traffic on port 5432. The
config map is similar to shown above.

[neon.tech]: https://neon.tech/

## Neon.tech

[Neon.tech][neon.tech] requires SSL connection so always pass the `:ssl?`
flag:

~~~clojure
(def config
  {:host "ep-fancy-queen-XXXXX.eu-central-1.aws.neon.tech"
   :port 5432
   :user "test_owner"
   :password "<password>"
   :database "test"
   :ssl? true ;; always mandatory!
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

See the [SSL Setup](/docs/ssl.md) section for details.
