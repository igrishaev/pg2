# Authentication

`PG2` supports the following authentication types and pipelines:

- No password (for trusted clients)

- Clear text password (not used nowadays)

- MD5 password with hash (default prior to Postgres 15)

- SASL with `SCRAM-SHA-256` (default since Postgres 15). `SCRAM-SHA-256-PLUS` is not implemented yet.
