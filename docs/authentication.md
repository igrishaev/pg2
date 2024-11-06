# Authentication

PG2 suppors the following authentication types and pipelines:

- No password (for trusted clients);

- Clear text password (unused nowadays);

- MD5 password with salt (default prior to Postgres ver. 15);

- SASL with SCRAM-SHA-256 algorithm (default since Postgres ver. 15). This
  authentication includes two pipelines: `SCRAM-SHA-256` and
  `SCRAM-SHA-256-PLUS`. The client is free to choose which algorithm to use. By
  default, PG2 prefers SCRAM-SHA-256 as it's a bit simpler than `-PLUS.` The
  `-PLUS` version requires SSL set to true:

~~~clojure
  {:host "some.host.com"
   :port 5432
   :user "test"
   :password "secret"
   :database "test"
   :use-ssl? true} ;; mandatory!
~~~
