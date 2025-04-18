# Type Mapping

PG2 provides the following bridge between Postgres and Clojure realms.

## Builtin Types

### Numbers

| Postgres | Reading    | Writing              |
|----------|------------|----------------------|
| int2     | Short      | Short, Integer, Long |
| int4     | Integer    | the same             |
| int8     | Long       | the same             |
| numeric  | BigDecimal | Most numeric types   |
| float4   | Float      | Float, Double        |
| float8   | Double     | the same             |

### Text

| Postgres | Reading   | Writing                         |
|----------|-----------|---------------------------------|
| varchar  | String    | String, UUID, Symbol, Character |
| text     | String    | the same                        |
| name     | String    | the same                        |
| bpchar   | String    | the same                        |
| regproc  | String    | the same                        |
| char     | Character | Character, 1-char String        |

### Geometry

See the [Geometry (line, box, etc)](/docs/geometry.md) section for more info.

| Postgres | Reading     | Writing                        |
|----------|-------------|--------------------------------|
| point    | Clojure map | Clojure map/vector, SQL string |
| line     | the same    | the same                       |
| box      | the same    | the same                       |
| circle   | the same    | the same                       |
| polygon  | the same    | the same                       |
| path     | the same    | the same                       |
| lseg     | the same    | the same                       |

### Misc

| Postgres | Reading           | Writing            |
|----------|-------------------|--------------------|
| uuid     | UUID              | UUID, String       |
| json     | any Clojure value | any Clojure value  |
| jsonb    | the same          | the same           |
| bytea    | byte[]            | byte[], ByteBuffer |
| boolean  | Boolean           | Boolean            |
| bit      | String            | byte[], String     |

### Date & Time

| Postgres    | Reading        | Writing                                            |
|-------------|----------------|----------------------------------------------------|
| timestamptz | OffsetDateTime | Most Temporal types (OffsetDateTime, Instant, etc) |
| timestamp   | LocalDateTime  | the same                                           |
| date        | LocalDate      | the same                                           |
| time        | LocalTime      | LocalTime, LocalTime                               |
| timetz      | OffsetTime     | OffsetTime, LocalTime                              |

### Arrays

PG2 supports arrays of all types mentioned above. They can have more than one
dimension which is useful sometimes for storing matrices. Names of array types
always start with an underscore: `_int4`, for example (an array of `int4`).

For details, read the [Arrays Support](/docs/arrays.md) section.

The table below renders **only a small subset** of supported arrays:

| Postgres           | Reading                  | Writing                  |
|--------------------|--------------------------|--------------------------|
| _int2              | Vector of short          | List of short/int/double |
| _uuid              | Vector of UUID           | List of UUID             |
| _timestamptz       | Vector of OffsetDateTime | List of OffsetDateTime   |
| ... (many of them) |                          |                          |

## Extensions

### PG Vector

See [PGVector Support](/docs/pgvector.md).

| Postgres  | Reading           | Writing             |
|-----------|-------------------|---------------------|
| vector    | Vector of doubles | Vector, List        |
| sparsevec | Clojure map       | Vector, Clojure map |

### Hstore

See [Hstore Support](/docs/hstore.md).

| Postgres | Reading     | Writing     |
|----------|-------------|-------------|
| hstore   | Clojure map | Clojure map |

## Enums

In Postgres, any enum type created in runtime gets its own id. That causes some
problems because, although it's nothing but text, its id differs from the
standard `text` type.

Each enum creates two types, in fact. The first one is an enum itself, and the
second is an array type. PG2 supports both of them.

For details, see the [Reading Postgres Types In Runtime](/docs/read-pg-types.md)
section.

| Postgres | Reading     | Writing                 | Comment              |
|----------|-------------|-------------------------|----------------------|
| foo      | String      | String, Keyword, Symbol | A plain enum value   |
| _foo     | Clojure map | Vector, Clojure map     | Array of enum values |
