# Working With Rows

When you `query` or `execute` something, by default, you get a vector of maps:

code query

These maps are not pure Clojure maps but rather special objects that mimic
Clojure maps, if fact. Namely, these are instances of the
`org.pg.clojure.RowMap` class that implements `APersistentMap` and some other
interfaces. But at first glance, it's a map indeed:

get assoc dissoc keys

The `RowMap` class is special in that term that it's lazy. Initially, it stores
only an array of keys and unparsed byte array that came from a Postgres
server. When you touch a certain key, a corresponding fragment of the array gets
parsed and cached. When you reach the same key the second time, it's taken from
a cache without parsing:

...

This feature brings two positive points. First, you don't spend time on parsing
rows when receiving data from a server. Thus, the throughput is faster, and the
connection is now busy for less time. You borrow a connection, get the data
without parsing and let other threads use it again.

Second, we often select more fields that is actually needed. For example,
passing `select * from table` with 20 fields while only two of three columns are
actually needed. With laziness, all 20 fields stay unparsed until you forcibly
trigger evaluation.

The `RowMap` class stays itself only while you're reading its keys. If you
`assoc` or `dissoc` a key, it gets transformed into a Clojure map. Thus, adding
or removing a key triggers a process of parsing all keys and making a fresh
Clojure map:

...

A `RowMap` instance knows the order of keys which gives two features. First, it
can be used as a vector using `nth` with fast access:

...

You can destruct a row map as a vector in `let` binding as follows:

The second feature is, it always preserves the order of columns when reading
keys, values, or processing a row as a sequence of key-value pairs. Here is a
small demo:

...

All these small features make data processing a bit easier and predictable.
