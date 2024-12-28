# Prepared Statements

In Postgres, prepared statements are queries that have passed all the
preliminary stages and now ready to be executed. Running the same prepared
statement with different parameters is faster than executing a fresh query each
time. To prepare a statement, pass a SQL expression into the `prepare`
function. It will return a special `PreparedStatement` object:

~~~clojure
(def stmt-by-id
  (pg/prepare conn "select * from test1 where id = $1"))

(str stmt-by-id)
<Prepared statement, name: s11, param(s): 1, OIDs: [INT4], SQL: select * from test1 where id = $1>
~~~

The statement might have parameters. Now that you have a statement, execute it
with the `execute-statement` function. Below, we execute it three times with
various primary keys. We also pass the `:first?` option set to true to have only
row in the result.

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

During its lifetime on the server, a statement consumes some resources. When
it's not needed any longer, release it with the `close-statement` function:

~~~clojure
(pg/close-statement conn stmt-by-id)
~~~

The following macro helps to auto-close a statement. The first argument is a
binding symbol. It will be pointing to a fresh prepared statement during the
execution of the body. Afterwards, the statement is closed.

Below, we insert tree rows in the database using the same prepared
statement. Pay attention to the `doall` clause: it evaluates the lazy sequence
produced by `for`. Without `doall`, you'll get an error from the server saying
there is no such a prepared statement.

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

**In Postgres, prepared statements are always bound to a certain
connection. Don't share a statement opened in a connection A to B and vice
versa. Do not share them across different threads.**

**UPD:** a recent version of PG2 has an internal cache of prepared
statements. See the [Prepared Statement Cache](/docs/prepared-statement-cache.md)
section for more info.
