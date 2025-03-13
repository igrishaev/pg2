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
