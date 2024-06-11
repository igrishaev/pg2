# JSON support

Postgres is amazing when dealing with JSON. There hardly can be a database that
serves it better. Unfortunately, Postgres clients never respect the JSON
feature, which is horrible. Take JDBC, for example: when querying a JSON(b)
value, you'll get a dull `PGObject` which should be decoded manually. The same
applies to insertion: one cannot just pass a Clojure map or a vector. It should
be packed into the `PGObject` as well.

Of course, this can be automated by extending certain protocols. But it's still
slow as it's done on Clojure level (not Java), and it forces you to copy the
same code across projects.

Fortunately, PG2 supports JSON out from the box. If you query a JSON value,
you'll get its Clojure counter-part: a map, a vector, etc. To insert a JSON
value to a table, you pass either a Clojure map or a vector. No additional steps
are required.

[jsonista]: https://github.com/metosin/jsonista

PG2 relies on [jsonista][jsonista] library to handle JSON. At the moment of
writing, this is the fastest JSON library for Clojure. Jsonista uses a concept
of object mappers: objects holding custom rules to encode and decode values. You
can compose your own mapper with custom rules and pass it into the connection
config.

## Basic usage

Let's prepare a connection and a test table with a jsonb column:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})

(def conn
  (jdbc/get-connection config))

(pg/query conn "create table test_json (
  id serial primary key,
  data jsonb not null
)")
~~~

Now insert a row:

~~~clojure
(pg/execute conn
            "insert into test_json (data) values ($1)"
            {:params [{:some {:nested {:json 42}}}]})
~~~

No need to encode a map manually nor wrap it into a sort of `PGObject`. Let's
fetch the new row by id:

~~~clojure
(pg/execute conn
            "select * from test_json where id = $1"
            {:params [1]
             :first? true})

{:id 1 :data {:some {:nested {:json 42}}}}
~~~

Again, the JSON data returns as a Clojure map with no wrappers.

When using JSON with HoneySQL though, some circs are still needed. Namely, you
have to wrap a value with `[:lift ...]` as follows:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data [:lift {:another {:json {:value [1 2 3]}}}]})

{:id 2, :data {:another {:json {:value [1 2 3]}}}}
~~~

Without the `[:lift ...]` tag, HoneySQL will treat the value as a nested SQL map
and try to render it as a string, which will fail of course or lead to a SQL
injection.

Another way is to use HoneySQL parameters conception:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data [:param :data]}
                {:honey {:params {:data {:some [:json {:map [1 2 3]}]}}}})
~~~

For details, see the [HoneySQL Integration](docs/honeysql.md) section.

PG2 supports only Clojure maps when encoding values into JSON. Vectors and other
sequential values are treated as arrays. For details, see the [Arrays
support](#arrays-support) section.

## Json Wrapper

In rare cases you might store a string or a number in a JSON field. Say, 123 is
a valid JSON value but it's treated as a number. To tell Postgres it's a JSON
indeed, wrap the value with `pg/json-wrap`:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data (pg/json-wrap 42)})

{:id 4, :data 42}
~~~

The wrapper is especially useful to store a "null" JSON value: not the standard
`NULL` but `"null"` which, when parsed, becomes `nil`. For this, pass
`(pg/json-wrap nil)` as follows:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data (pg/json-wrap nil)})

{:id 5, :data nil} ;; "null" in the database
~~~

## Custom Object Mapper

One great thing about Jsonista is a conception of mapper objects. A mapper is a
set of rules how to encode and decode data. Jsonista provides a way to build a
custom mapper. Once built, it can be passed to a connection config so the JSON
data is written and read back in a special way.

Let's assume you're going to tag JSON sub-parts to track their types. For
example, if encoding a keyword `:foo`, you'll get a vector of `["!kw",
"foo"]`. When decoding that vector, by the `"!kw"` string, the mapper
understands it a keyword and coerces `"foo"` to `:foo`.

Here is how you create a mapper with Jsonista:

~~~clojure

(ns ...
  (:import
   clojure.lang.Keyword
   clojure.lang.PersistentHashSet)
  (:require
    [jsonista.core :as j]
    [jsonista.tagged :as jt]))

(def tagged-mapper
  (j/object-mapper
   {:encode-key-fn true
    :decode-key-fn true
    :modules
    [(jt/module
      {:handlers
       {Keyword {:tag "!kw"
                 :encode jt/encode-keyword
                 :decode keyword}
        PersistentHashSet {:tag "!set"
                           :encode jt/encode-collection
                           :decode set}}})]}))
~~~

The `object-mapper` function accepts even more options but we skip them for now.

Now that you have a mapper, pass it into a config:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"
   :object-mapper tagged-mapper})

(def conn
  (jdbc/get-connection config))
~~~

All the JSON operations made by this connection will use the passed object
mapper. Let's insert a set of keywords:

~~~clojure
(pg/execute conn
            "insert into test_json (data) values ($1)"
            {:params [{:object #{:foo :bar :baz}}]})
~~~

When read back, the JSON value is not a vector of strings any longer but a set
of keywords:

~~~clojure
(pg/execute conn "select * from test_json")

[{:id 1, :data {:object #{:baz :bar :foo}}}]
~~~

To peek a raw JSON value, select it as a plain text and print (just to avoid
escaping quotes):

~~~clojure
(printl (pg/execute conn "select data::text json_raw from test_json where id = 10"))

;; [{:json_raw {"object": ["!set", [["!kw", "baz"], ["!kw", "bar"], ["!kw", "foo"]]]}}]
~~~

If you read that row using another connection with a default object mapper, the
data is returned without expanding tags.

## Utility pg.json namespace

PG2 provides an utility namespace for JSON encoding and decoding. You can use it
for files, HTTP API, etc. If you already have PG2 in the project, there is no
need to plug in Cheshire or another JSON library. The namespace is `pg.json`:

~~~clojure
(ns ...
  (:require
   [pg.json :as json]))
~~~

### Reading JSON

The `read-string` function reads a value from a JSON string:

~~~clojure
(json/read-string "[1, 2, 3]")

[1 2 3]
~~~

The first argument might be an object mapper:

~~~clojure
(json/read-string tagged-mapper "[\"!kw\", \"hello\"]")

:hello
~~~

The functions `read-stream` and `read-reader` act the same but accept either an
`InputStream` or a `Reader` object:

~~~clojure
(let [in (-> "[1, 2, 3]" .getBytes io/input-stream)]
  (json/read-stream tagged-mapper in))

(let [in (-> "[1, 2, 3]" .getBytes io/reader)]
  (json/read-reader tagged-mapper in))
~~~

### Writing JSON

The `write-string` function dumps an value into a JSON string:

~~~clojure
(json/write-string {:test [:hello 1 true]})

;; "{\"test\":[\"hello\",1,true]}"
~~~

The first argument might be a custom object mapper. Let's reuse our tagger
mapper:

~~~clojure
(json/write-string tagged-mapper {:test [:hello 1 true]})

;; "{\"test\":[[\"!kw\",\"hello\"],1,true]}"
~~~

The functions `write-stream` and `write-writer` act the same. The only
difference is, they accept either an `OutputStream` or `Writer` objects. The
first argument might be a mapper as well:

~~~clojure
(let [out (new ByteArrayOutputStream)]
  (json/write-stream tagged-mapper {:foo [:a :b :c]} out))

(let [out (new StringWriter)]
  (json/write-writer tagged-mapper {:foo [:a :b :c]} out))
~~~

## Ring HTTP middleware

[ring-json]: https://github.com/ring-clojure/ring-json

PG2 provides an HTTP Ring middleware for JSON. It acts like `wrap-json-request`
and `wrap-json-response` middleware from the [ring-json][ring-json]
library. Comparing to it, the PG2 stuff has the following advantages:

- it's faster because of Jsonista, whereas Ring-json relies on Cheshire;
- it wraps both request and response at once with a shortcut;
- it supports custom object mappers.

Imagine you have a Ring handler that reads JSON body and returns a JSON
map. Something like this:

~~~clojure
(defn api-handler [request]
  (let [user-id (-> request :data :user_id)
        user (get-user-by-id user-id)]
    {:status 200
     :body {:user user}}))
~~~

Here is how you wrap it:

~~~clojure
(ns ...
  (:require
   [pg.ring.json :refer [wrap-json
                         wrap-json-response
                         wrap-json-request]]))

(def app
  (-> api-handler
      (wrap-this-foo)
      (wrap-json <opt>)
      (wrap-that-bar)))
~~~

Above, the `wrap-json` wrapper is a combination of `wrap-json-request` and
`wrap-json-response`. You can apply them both explicitly:

~~~clojure
(def app
  (-> api-handler
      (wrap-this-foo)
      (wrap-json-request <opt>)
      (wrap-json-response <opt>)
      (wrap-that-bar)))
~~~

All the three `wrap-json...` middleware accept a handler to wrap and a map of
options. Here is the options supported:

| Name                  | Direction         | Description                                                |
|-----------------------|-------------------|------------------------------------------------------------|
| `:object-mapper`      | request, response | An custom instance of `ObjectMapper`                       |
| `:slot`               | request           | A field to `assoc` the parsed JSON data (1)                |
| `:malformed-response` | request           | A ring response returned when payload cannot be parsed (2) |

Notes:

1. The default slot name is `:json`. Please avoid using `:body` or `:params` to
   prevent overriding existing request fields. This is especially important for
   `:body`! Often, you need the origin input stream to calculate an MD5 or
   SHA-256 hash-sum of the payload. If you overwrite the `:body` field, you
   cannot do that.

2. The default malformed response is something like 400 "Malformed JSON" (plain
   text).

A full example:

~~~clojure
(def json-opt
  {:slot :data
   :object-mapper tagged-mapper ;; see above
   :malformed-response {:status 404
                        :body "<h1>Bad JSON</h1>"
                        :headers {"content-type" "text/html"}}})

(def app
  (-> api-handler
      (wrap-this-foo)
      (wrap-json json-opt)
      (wrap-that-bar)))
~~~~~~
