# Prepared Statements

In Postgres prepared statements are queries that have passed all the
preliminary stages and now ready to be executed. Running the same prepared
statement with different parameters is faster than executing a fresh query each
time. To prepare statement pass SQL expression into `prepare`
function. It will return a special `PreparedStatement` object:

~~~clojure
(def stmt-by-id
  (pg/prepare conn "select * from test1 where id = $1"))

(str stmt-by-id)
<Prepared statement, name: s11, param(s): 1, OIDs: [INT4], SQL: select * from test1 where id = $1>
~~~

Statements can have parameters. Now that you have a statement execute it
with `execute-statement`. Below we execute it with
various primary keys, we also pass `:first? true` to return only one row.

~~~clojure
(pg/execute-statement conn
                      stmt-by-id
                      {:params [1] :first? true})
;; {:name "Ivan", :id 1}

(pg/execute-statement conn
                      stmt-by-id
                      {:params [5] :first? true})
;; {:name "Louie", :id 5}

(pg/execute-statement conn
                      stmt-by-id
                      {:params [8] :first? true})
;; {:name "Agent Smith", :id 8}
~~~

During its lifetime on the server statement consumes some resources, release it with `close-statement` when it's no longer needed:

~~~clojure
(pg/close-statement conn stmt-by-id)
~~~

The following macro auto-closes statement. The first argument is a
binding symbol. It will be pointing to a freshly prepared statement during the
execution of the body. Afterwards the statement will be closed.

Below we insert tree rows in the database using the same prepared
statement. Pay attention to the `doall` clause: it evaluates the lazy sequence
produced by `for`. Without `doall` you'll get an error from the server saying
there is no such prepared statement.

~~~clojure
(pg/with-statement [stmt conn "insert into test1 (name) values ($1) returning *"]
  (doall
   (for [character ["Agent Brown"
                    "Agent Smith"
                    "Agent Jones"]]
     (pg/execute-statement conn stmt {:params [character] :first? true}))))

({:name "Agent Brown", :id 12}
 {:name "Agent Smith", :id 13}
 {:name "Agent Jones", :id 14})
~~~

**In Postgres prepared statements are always bound to a certain
connection.
Don't share statements between connections.
Don't share them across different threads.**
