# Custom Type Processors

PG2 version 0.1.18 has the entire type system refactored. It introduces a
conception of type processors which allows to connect Postgres types with
Java/Clojure ones with ease.

When reading data from Postgres, the client knows only the OID of a type of a
column. This OID is just an integer number points to a certain type. The default
builtin types are hard-coded in Postgres, and thus their OIDs are known in
advance.

Say, it's for sure that the `int4` type has OID 23, and `text` has
OID 25. That's true for any Postgres installation. Any Postgres client has a
kind of a hash map or a Enum class with these OIDs.

Things get worse when you define custom types. These might be either enums or
complex types defined by extensions: `pgvector`, `postgis` and so on. You cannot
guess OIDs of types any longer because they are generated in runtime. Their
actual values depend on a specific machine. On prod, the `public.vector` type
has OID 10541, on pre-prod it's 9621, and in Docker you'll get 1523.

Moreover, a type name is unique only across a schema that's holding it. You can
easily have two different enum types called `status` defined in various
schemas. Thus, relying on a type name is not a good option unless it's fully
qualified.

To deal with all said above, a new conception of type mapping was introduced.

First, if a certain OID is builtin (meaning it exists the list of predefined
OIDs), it gets processed as before.

When you connect to a database, you can pass a mapping like `{schema.typename =>
Processor}`. When pg2 has established a connection, it executes an internal
query to discover type mapping. Namely, it reads the `pg_type` table to get OIDs
that have provided schemas and type name. The query looks like this:

~~~sql
select
    pg_type.oid, pg_namespace.nspname || '.' || pg_type.typname as type
from
    pg_type, pg_namespace
where
    pg_type.typnamespace = pg_namespace.oid
    and pg_namespace.nspname || '.' || pg_type.typname in (
        'schema1.type1',
        'schema2.type2',
        ...
    );
~~~

It returns pairs of OID and the full type name:

~~~
121512 | schema1.type1
 21234 | schema2.type2
~~~

Now PG2 knows that the OID 121512 specifies `schema1.type1` but nothing else.

Finally, from the map `{schema.typename => Processor}` you submitted before, PG2
builds a map `{OID => Processor}`. If the OID is not a default one, it checks
this map trying to find a processor object.

A processor object is an instance of the `org.pg.processor.IProcessor`
interface, or, if more precisely, an abstract `AProcessor` which is partially
implemented. It has four methods:

~~~java
ByteBuffer encodeBin(Object value,  CodecParams codecParams);
    String encodeTxt(Object value,  CodecParams codecParams);
    Object decodeBin(ByteBuffer bb, CodecParams codecParams);
    Object decodeTxt(String text,   CodecParams codecParams);
~~~

Depending on whether you're decoding (reading) the data or encoding them
(e.g. passing parameters), and the current format (text or binary), a
corresponding method is called. By extending all four methods, you can handle
any type you want.

At the moment, there are about 25 processors implementing standard types:
`int2`, `int4`, `text`, `float4`, and so on. Find them in the
`pg-core/src/java/org/pg/processor` directory. There is also a couple of
processors for the `pgvector` extension in the `pgvector` subdirectory.

See an example of passing processors explicitly in the [pgvector
section](/docs/pgvector.md).

The next step is to implement processors for the `postgis` extension.
