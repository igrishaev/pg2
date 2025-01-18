# SSL Setup

Some PostgreSQL installations require connections to be protected with SSL. Most
cloud providers allow non-SSL connections only when reaching the database from
the cloud. But when you connect from the outer world (say, your office), SSL is
mandatory. Reaching such a database without SSL results in the following error
from the server:

~~~text
severity=FATAL, code=XX000, message=SSL connection is required
~~~

Setting up SSL certificates in Java is a bit miserable, but PG2 provides
workarounds.

## Case 1: SSL Without Certificates

Some cloud providers require SSL connection but do not share any certificate
files. In that case, just pass the boolean `:ssl?` flag to the
configuration, and that will be enough:

~~~clojure
(def config
  {:host "some.host.com"
   :port 5432
   :user "ivan"
   :password "<password>"
   :database "test"
   :ssl? true ;; this one
   })
~~~

## Case 2: a Single Ca Certificate Validation

Other providers share one CA certificate file, meaning your JVM must trust it. A
good example is Supabase.com who lets you decide if the database is
SSL-protected. When the checkbox is set, there is a button to download a file
called `prod.ca-2021.crt`:

![](/media/supabase.png)

Once you've got this file, initiate a custom `SSLContext` instance out from
it. Use the `pg.ssl` namespace and the `context` function:

~~~clojure
(require '[pg.ssl :as ssl])

(def ssl-context
  (ssl/context "/Users/ivan/Downloads/prod-ca-2021.crt"))
~~~

Add this context into the config map under the `:ssl-context` key. Passing a
custom instance of `SSLContext` automatically enables the `:ssl?` flag as
well:

~~~clojure
(def config
  {:host "some.host.com"
   :port 5432
   :user "ivan"
   :password "<password>"
   :database "postgres"
   :ssl-context ssl-context ;; this
   })
~~~

## Case 3: Key File and Cert Files

A provider may share two files with you called `client.key` and
`client.crt`. The `context` function from `pg.ssl` has a special two-arity body
which accepts them:

~~~clojure
(ssl/context "../certs/client.key"
             "../certs/client.crt")
~~~

Pass the result into the `:ssl-context` config field.


## Case 3: Key, Cert, and CA/Root files

The same as above but with an extra CA/root certificate file (might be called
`cert-CA`, `root-ca`, or just `root`). Pass them in the same order into the
`context` function to make an SSL context:

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

Once certificates are imported, they're held by Java so you only specify `:ssl?`
flag without custom instances of `SSLContext`.

Pay attention there is a chance to mess up. Imagine you import cerficiates into
a keystore related to a JVM that you use for REPL. But when running an uberjar,
another JVM is used, and it lacks those certificates. When passing them
explicitly in code, there won't be such an error.

[jdbc-ssl]: https://jdbc.postgresql.org/documentation/ssl/

You may find more details about setting up SSL and JVM keystore in [this
article][jdbc-ssl].

Also see the related ["Services Tested With"](/docs/services.md) section with
real examples of SSL configurations.

[less-awful-ssl]: https://github.com/aphyr/less-awful-ssl

**Gratitude:** in PG2, SSL is handled using the great [Less Awful
SSL][less-awful-ssl] library. Thank you Kyle!
