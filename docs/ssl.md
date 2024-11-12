# SSL Setup

Some PostgreSQL installations require connections to be protected with SSL. It's
an often case for cloud providers when a database is reachable without SSL only
from insides of the cloud. But when you connect from the outer world (say, your
office), SSL is mandatory.

When connecting a database with requires SSL without it, you'll get an error
response from the server:

~~~text
severity=FATAL, code=XX000, message=SSL connection is required
~~~

Setting up SSL certificates in Java is a mess, yet PG2 provides some workarounds
for that.

## Case 1: SSL Without Certificates

Some cloud providers require SSL connection but do not share any certificate
files. In that case, just pass the boolean `:use-ssl?` flag to the
configuration:

~~~clojure
(def config
  {:host "some.host.com"
   :port 5432
   :user "ivan"
   :password "<password>"
   :database "test"
   :use-ssl? true})
~~~

## Case 2: a Single Ca Certificate Validation

Other providers share just one CA certificate file, meaning your JVM must trust
this certificate chain. A good example is `supabase.com` provider who lets you
an option to protect the database with SSL. When this checkbox is set, there is
a button to download the `prod.ca-2021.crt` file:

TODO

Once you've got this file, initiate a custom SSLContext instance out from this
file. Use the `pg.ssl` namespace:

~~~clojure
(require '[pg.ssl :as ssl])

(def ssl-context
  (ssl/context "/Users/ivan/Downloads/prod-ca-2021.crt"))
~~~

Then pass this context into the config map. Passing a custom instance of
`SSLContext` automatically enables the `:use-ssl?` flag as well.

~~~clojure
(def config
  {:host "some.host.com"
   :port 5432
   :user "ivan"
   :password "<password>"
   :database "postgres"
   :ssl-context ssl-context})
~~~

## Case 3: Key File and Cert Files

These two files are usually called `client.key` and `client.crt`. The `context`
function from `pg.ssl` has a special two-arity body which accepts them:

~~~clojure
(ssl/context "../certs/client.key"
             "../certs/client.crt")
~~~

## Case 3: Key, Cert, and CA/Root files

The same as above but with an extra CA/root certificate file. Pass them in the
same order into the `context` function:

~~~clojure
(ssl/context "../certs/client.key"
             "../certs/client.crt"
             "../certs/ca-or-root.crt")
~~~

## Case 4: Importing certificates into JVM

You may let JVM to handle certificates for you. Importing them into JVM boils
down to the following bash command:

~~~bash
keytool -keystore mystore -alias postgresql -import -file ...
~~~

Once certificates are imported, they're held by Java so you only specify
`:use-ssl?` flag without a custom instance of `SSLContext`.

Pay attention there is a chance to mess up: you import cerficiates into a
keystore related to a JVM you use for REPL. But when running an uberjar, another
JVM might be used which lacks those certificates. When passing them explicitly
via config, there won't be such an error.

You may find more details about setting up SSL and JVM keystore in [this
article][jdbc-ssl].

Also see the related ["Services Tested With"](/docs/services.md) section with
real examples of SSL configurations.
