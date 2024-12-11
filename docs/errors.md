# Errors and Exceptions

PG2 introduces two kinds of exceptions: `PGError` and `PGErrorResponse`.

A `PGError` exception happens on the client side due to wrong values or data
encoding errors. For example, below, the exception pops up because the library
doesn't know how to encode an `Object` instance into an integer:

~~~clojure
(def stmt
  (pg/prepare conn "select * from users where id = $1"))

(pg/execute-statement conn stmt {:params [(new Object)]})

;; PGError: cannot coerce value to long: type: java.lang.Object...
~~~

A `PGErrorResponse` exception happens when Postgres responds with the
`ErrorResponse` message. Usually it means you have managed to reach a database
but it didn't like what you sent. It might be an issue syntax, wrong parameters,
or whatever else.

The `ErrorResponse` message carries plenty of fields describing an error, and
the `PGErrorResponse` class relies on them. Namely, the error message renders
all the fields received from the server:

~~~clojure
(pg/execute conn "selekt 1")

;; PGErrorResponse
;; Server error response: {severity=ERROR, code=42601, file=scan.l,
;; line=1145,  function=scanner_yyerror, position=1,
;; message=syntax error at or near \"selekt\", verbosity=ERROR}, sql: selekt 1
~~~

Pay attention that the error message tracks a SQL expression that triggered an
error on the server side. Above, it's the "sql: selekt 1" part of the
message. Long SQL expressions that exceed 128 characters get truncated.

As the `PGErrorResponse` class implements the `clojure.lang.IExceptionInfo`
interface, it's compatible with the `ex-data` function. Passing an instance of
`PGErrorResponse` into this function returns a Clojure map with all the fields
and the full, non-truncated SQL expression:

~~~clojure
(try
  (pg/execute conn "selekt 1")
  (catch PGErrorResponse e
    (ex-data e)))

{:verbosity "ERROR"
 :file "scan.l"
 :function "scanner_yyerror"
 :sql "selekt 1"
 :line "1145"
 :severity "ERROR"
 :code "42601"
 :position "1"
 :message "syntax error at or near \"selekt\""}
~~~

Why is it needed? In some rare cases, you rely on the `:code` field, for example
retry a transaction that has broken previously due to a conflict:

~~~clojure
(try
  (conn/execute conn "do complex stuff")
  (catch PGErrorResponse e
    (let [code (-> e ex-data :code)]
      (if (= code "123456") ;; a special code from Postgres docs
        (retry-logic-goes-here conn)
        (throw e) ;; re-throw it
        ))))
~~~
