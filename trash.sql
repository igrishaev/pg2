select
        pg_type.oid,
        pg_type.typname,
        pg_type.typtype,
        pg_type.typinput::text,
        pg_type.typoutput::text,
        pg_type.typreceive::text,
        pg_type.typsend::text,
        pg_type.typarray,
        pg_type.typdelim,
        pg_type.typelem,
        pg_namespace.nspname
    from
        pg_type,
        pg_namespace
    where
        pg_type.typnamespace = pg_namespace.oid
        and pg_type.oid = 16386
        -- and pg_namespace.nspname != 'pg_catalog'
        -- and pg_namespace.nspname != 'information_schema'


copy (
    select
        pg_type.oid,
        pg_type.typname,
        pg_type.typtype,
        pg_type.typinput::text,
        pg_type.typoutput::text,
        pg_type.typreceive::text,
        pg_type.typsend::text,
        pg_type.typarray,
        pg_type.typdelim,
        pg_type.typelem,
        pg_namespace.nspname
    from
        pg_type,
        pg_namespace
    where
        pg_type.typnamespace = pg_namespace.oid
        and pg_namespace.nspname != 'pg_catalog'
        and pg_namespace.nspname != 'information_schema'
) to stdout with (format csv)




test=# select * from pg_type where oid = 27571;
-[ RECORD 1 ]--+-----------------------
oid            | 27571
typname        | triple_851965879281416
typnamespace   | 2200
typowner       | 10
typlen         | -1
typbyval       | f
typtype        | c
typcategory    | C
typispreferred | f
typisdefined   | t
typdelim       | ,
typrelid       | 27569


test=# select * from pg_attribute where attrelid = 27569;
test=# select array_agg(attname), array_agg(atttypid) from pg_attribute where attrelid = 27569;
-[ RECORD 1 ]---------
array_agg | {a,b,c}
array_agg | {23,25,16}
