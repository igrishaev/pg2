# Get & Set Runtime Parameters

Postgres provides a number of parameters that you change in runtime, e.g. for
the current connection. Usually we use `SET` and `SHOW` commands for this as
follows:

~~~sql
-- get the name
show application_name;
 application_name
------------------
 psql
(1 row)

-- set the name
set application_name to pg_test;
SET

-- get the new value
show application_name;
 application_name
------------------
 pg_test
(1 row)
~~~

It's a bit inconvenient to call these commands directly from PG2 as they don't
support parameters. Thus, you have to format or concatenate strings which leads
to SQL injections.

A recent release of PG2 ships special functions to get and set a parameter by
its name. The **`current-setting`** function gets a parameter:

~~~clojure
(pg/with-connection [conn config]
  (pg/current-setting conn "application_name"))

;; pg2
~~~

Should you specify a missing parameter, the function returns `nil`:

~~~clojure
(pg/current-setting conn "dunno")
;; nil
~~~

This can be altered by passing an extra `missing-ok?` boolean parameter (`true`
by default) checking if a missing parameter should lead to an exception:

~~~clojure
(pg/current-setting conn "dunno" false)
;; throws PGErrorResponse
;; ... unrecognized configuration parameter "dunno"
~~~

The **`set-config`** function sets a new value for an existing parameter:

~~~clojure
(pg/set-config conn "application_name" "A_B_C")
(pg/current-setting conn "application_name")
;; A_B_C
~~~

Setting a missing parameter leads to an error on the server side.

By default, `set-config` changes the parameter forever (for the current
connection of course). Sometimes you need to alter it only inside a
transaction. The last optional parameter (`false` by default) checks if the
parameter should be local (per a transaction):

~~~clojure
(pg/current-setting conn "application_name")
;; A_B_C

(pg/with-tx [conn]
  (pg/set-config conn "application_name" "xxx-yyy" true)
  (pg/current-setting conn "application_name")
;; xxx-yyy

(pg/current-setting conn "application_name")
;; A_B_C
~~~

These Clojure functions rely on Postgres functions called `set_config` and
`current-setting`. Both functions work with parameters meaning they are safe
from SQL injections.
