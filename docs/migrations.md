# Migrations

PG2 provides its own migration engine through the `pg2-migration` package (see
[Installation](#installation)). Like Migratus or Ragtime, it allows to grow the
database schema continuously, track changes and apply them with care.

## Concepts

Migrations are SQL files that are applied to the database in certain order. A
migration has an id and a direction: next/up or prev/down. Usually it's split on
two files called `<id>.up.sql` and `<id>.down.sql` holding SQL commands. Say,
the -up file creates a table with an index, and the -down one drops the index
first, and then the table.

Migrations might have a slug: a short human-friendly text describing
changes. For example, in a file called `002.create-users-table.up.sql`, the slug
is "Create users table".

## Naming

In PG2, the migration framework looks for files matching the following pattern:

~~~
<id>.<slug>.<direction>.sql
~~~

where:

- `id` is a Long number, for example 12345 (a counter), or 20240311 (date
  precision), or 20240311235959 (date & time precision);

- `slug` is an optional word or group of words joined with `-` or `_`, for
  example `create-users-table-and-index` or `remove_some_view`. When rendered,
  both `-` and `_` are replaced with spaces, and the phrase is capitalized.

- `direction` is either `prev/down` or `next/up`. Internally, `down` and `up`
  are transformed to `prev` and `next` because these two have the same amount of
  characters and files look better.

Examples:

- `001.create-users.next.sql`
- `012.next-only-migration.up.sql`
- `153.add-some-table.next.sql`

Above, the leading zeroes in ids are used for better alignment only. Infernally
they are transferred into 1, 12 and 153 Long numbers. Thus, `001`, `01` and `1`
become the same id 1 after parsing.

Each id has at most two directions: prev/down and next/up. On bootstrap, the
engine checks it to prevent weird behaviour. The table below shows there are two
rows which, after parsing, have the same (id, direction) pair. The bootstrap
step will end up with an exception saying which files duplicate each other.

| Filename                         | Parsed    |
|----------------------------------|-----------|
| `001.some-trivial-slug.next.sql` | (1, next) |
| `001.some-simple-slug.next.sql`  | (1, next) |

A migration might have only one direction, e.g. next/up or prev/down file only.

When parsing, the registry is ignored meaning that both
`001-Create-Users.NEXT.sql` and `001-CREATE-USERS.next.SQL` files produce the
same map.

## SQL

The files hold SQL expressions to be evaluated by the engine. Here is the
content of the `001.create-users.next.sql` file:

~~~sql
create table IF NOT EXISTS test_users (
  id serial primary key,
  name text not null
);

BEGIN;

insert into test_users (name) values ('Ivan');
insert into test_users (name) values ('Huan');
insert into test_users (name) values ('Juan');

COMMIT;
~~~

Pay attention to the following points.

- A single file might have as many SQL expressions as you want. There is no need
  to separate them with magic comments like `--;;` as Migratus requires. The
  whole file is executed in a single query. Use the standard semicolon at the
  end of each expression.

- There is no a hidden transaction management. Transactions are up to you: they
  are explicit! Above, we wrap tree `INSERT` queries into a single
  transaction. You can use save-points, rollbacks, or whatever you want. Note
  that not all expressions can be in a transaction. Say, the `CREATE TABLE` one
  cannot and thus is out from the transaction scope.

For granular transaction control, split your complex changes on two or three
files named like this:

```
# direct parts
001.huge-update-step-1.next.sql
002.huge-update-step-2.next.sql
003.huge-update-step-3.next.sql

# backward counterparts
003.huge-update-step-3.prev.sql
002.huge-update-step-2.prev.sql
001.huge-update-step-1.prev.sql
```

## No Code-Driven Migrations

At the moment, neither `.edn` nor `.clj` migrations are supported. This is by
design because personally I'm highly against mixing SQL and Clojure. Every time
I see an EDN transaction, I get angry. Mixing these two for database management
is the worst idea one can come up with. If you're thinking about migrating a
database with Clojure, please close you laptop and have a walk to the nearest
park.

## Migration Resources

Migration files are stored in project resources. The default search path is
`migrations`. Thus, their physical location is `resources/migrations`. The
engine scans the `migrations` resource for children files. Files from nested
directories are also taken into account. The engine supports Jar resources when
running the code from an uberjar.

The resource path can be overridden with settings.

## Migration Table

All the applied migrations are tracked in a database table called `migrations`
by default. The engine saves the id and the slug or a migration applied as well
as the current timestamp of the event. The timestamp field has a time zone. Here
is the structure of the table:

~~~sql
CREATE TABLE IF NOT EXISTS migrations (
  id BIGINT PRIMARY KEY,
  slug TEXT,
  created_at timestamp with time zone not null default current_timestamp
)
~~~

Every time you apply a migration, a new record is inserted into the table. On
rollback, a corresponding migration is deleted.

You can override the name of the table in settings (see below).

## CLI Interface

The migration engine is controlled with both API and CLI interface. Let's review
CLI first.

The `pg.migration.cli` namespaces acts like the main entry point. It accepts
general options, a command, and command-specific options:

```
<global options> <command> <command options>
```

General options are:

```
-c, --config CONFIG                                Path to an .edn config (a resource or a local file)
-p, --port PORT          5432                      Port number
-h, --host HOST          localhost                 Host name
-u, --user USER          The current USER env var  User
-w, --password PASSWORD  <empty string>            Password
-d, --database DATABASE  The current USER env var  Database
    --table TABLE        :migrations               Migrations table
    --path PATH          migrations                Migrations path (a resource or a local file)
```

Most of the options have default values. Both user and database names come from
the `USER` environment variable. The password is an empty string by default. For
local trusted connections, the password might not be required.

The list of the commands:

| Name     | Meaning                                                         |
|----------|-----------------------------------------------------------------|
| create   | Create a pair of blank up & down migration files                |
| help     | Print a help message                                            |
| list     | Show all the migrations and their status (applied or not)       |
| migrate  | Migrate forward (everything, next only, or up to a certain ID)  |
| rollback | Rollback (the current one, everything, or down to a certain ID) |

Each command has its own sub-options which we will describe below.

Here is how you review the migrations:

~~~
<lein or deps preamble> \
    -h 127.0.0.1 \
    -p 10150 \
    -u test \
    -w test \
    -d test \
    --table migrations_test \
    --path migrations \
    list

|    ID | Applied? | Slug
| ----- | -------- | --------
|     1 | true     | create users
|     2 | false    | create profiles
|     3 | false    | next only migration
|     4 | false    | prev only migration
|     5 | false    | add some table
~~~

Every command has its own arguments and help message. For example, to review the
`create` command, run:

~~~
lein with-profile +migrations run -m pg.migration.cli -c config.example.edn create --help

Syntax:
      --id ID             The id of the migration (auto-generated if not set)
      --slug SLUG         Optional slug (e.g. 'create-users-table')
      --help       false  Show help message
~~~

## Config

Passing `-u`, `-h`, and other arguments all the time is inconvenient. The engine
can read them at once from a config file. The default config location is
`migration.config.edn`. Override the path to the config using the `-c`
parameter:

~~~
<lein/deps> -c config.edn list
~~~

The config file has the following structure:

~~~clojure
{:host "127.0.0.1"
 :port 10150
 :user "test"
 :password #env PG_PASSWORD
 :database "test"
 :migrations-table :migrations_test
 :migrations-path "migrations"}
~~~

The `:migrations-table` field must be a keyword because it takes place in a
HoneySQL map.

The `:migrations-path` field is a string referencing a resource with migrations.

Pay attention to the `#env` tag. The engine uses custom readers when loading a
config. The tag reads the actual value from an environment variable. Thus, the
database password won't be exposed to everyone. When the variable is not set, an
exception is thrown.

## Commands

### Create

The `create` command makes a pair of two blank migration files. If not set, the
id is generated automatically using the `YYYYmmddHHMMSS` pattern.

~~~
lein with-profile +migration run -m pg.migration.cli \
  -c config.example.edn \
  create

ls -l migrations

20240312074156.next.sql
20240312074156.prev.sql
~~~

You can also provide a custom id and a slug as well:

~~~
lein with-profile +migration run -m pg.migration.cli \
  -c config.example.edn \
  create \
  --id 100500 \
  --slug 'some huge changes in tables'

ll migrations

100500.some-huge-changes-in-tables.next.sql
100500.some-huge-changes-in-tables.prev.sql
20240312074156.next.sql
20240312074156.prev.sql
~~~

### List

The `list` command renders all the migrations and their status: whether they are
applied or not.

~~~clojure
lein with-profile +migration run -m pg.migration.cli -c config.example.edn list

|    ID | Applied? | Slug
| ----- | -------- | --------
|     1 | true     | create users
|     2 | true     | create profiles
|     3 | true     | next only migration
|     4 | false    | prev only migration
|     5 | false    | add some table
~~~

### Migrate

The `migrate` command applies migrations to the database. By default, all the
pending migrations are processed. You can change this behaviour using these
flags:

~~~
... migrate --help

Syntax:
      --all           Migrate all the pending migrations
      --one           Migrate next a single pending migration
      --to ID         Migrate next to certain migration
      --help   false  Show help message
~~~

With the `--one` flag set, only one next migration will be applied. If `--to`
parameter is set, only migrations up to this given ID are processed. Examples:

~~~bash
... migrate           # all migrations
... migrate --all     # all migrations
... migrate --one     # next only
... migrate --to 123  # all that <= 123
~~~

### Rollback

The `rollback` command reverses changes in the database and removes
corresponding records from the migration table. By default, only the current
migration is rolled back. Syntax:

~~~
... rollback --help

Syntax:
      --all           Rollback all the previous migrations
      --one           Rollback to the previous migration
      --to ID         Rollback to certain migration
      --help   false  Show help message
~~~

The `--one` argument is the default behaviour. When `--all` is passed, all the
backward migrations are processed. To rollback to a certain migration, pass
`--to ID`. Examples:

~~~
... rollback               # current only
... rollback --one         # current only
... rollback --to 20240515 # down to 20240515
... rollback --all         # down to the very beginning
~~~

## Lein examples

Lein preamble looks usually something like this:

~~~
> lein run -m pg.migration.cli <ARGS>
~~~

The `pg2-migration` library must be in dependencies. Since migrations are
managed aside from the main application, they're put into a separate profile,
for example:

~~~clojure
:profiles
{:migrations
 {:main pg.migration.cli
  :resource-paths ["path/to/resources"]
  :dependencies
  [[com.github.igrishaev/pg2-core ...]]}}
~~~

Above, the `migrations` profile has the dependency and the `:main`
attribute. Now run `lein run` with migration arguments:

~~~bash
> lein with-profile +migrations run -c migration.config.edn migrate --to 100500
~~~

## Deps.edn examples

Here is an example of an alias in `deps.edn` that prints pending migrations:

~~~clojure
{:aliases
 {:migrations-list
  {:extra-deps
   {com.github.igrishaev/pg2-migration {:mvn/version "..."}}
   :extra-paths
   ["test/resources"]
   :main-opts
   ["-m" "pg.migration.cli"
    "-h" "127.0.0.1"
    "-p" "10150"
    "-u" "test"
    "-w" "test"
    "-d" "test"
    "--table" "migrations_test"
    "--path" "migrations"
    "list"]}}}
~~~

Run it as follows:

~~~
> clj -M:migrations-list
~~~

You can shorten it by using the config file. Move all the parameters into the
`migration.config.edn` file, and keep only a command with its sub-arguments in
the `:main-opts` vector:

~~~clojure
{:aliases
 {:migrations-migrate
  {:extra-deps
   {com.github.igrishaev/pg2-migration {:mvn/version "..."}}
   :extra-paths
   ["test/resources"]
   :main-opts ["migrate" "--all"]}}}
~~~

To migrate:

~~~
> clj -M:migrations-migrate
~~~

## API Interface

There is a way to manage migrations through code. The `pg.migration.core`
namespace provides basic functions to list, create, migrate, and rollback
migrations.

To migrate, call one of the following functions: `migrate-to`, `migrate-all`,
and `migrate-one`. All of them accept a config map:

~~~clojure
(ns demo
  (:require
   [pg.migration.core :as mig]))

(def CONFIG
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "secret"
   :database "test"
   :migrations-table :test_migrations
   :migrations-path "migrations"})

;; migrate all pinding migrations
(mig/migrate-all CONFIG)

;; migrate only one next migration
(mig/migrate-one CONFIG)

;; migrate to a certain migration
(mig/migrate-to CONFIG 20240313)
~~~

The same applies to rollback:

~~~clojure
;; rollback all previously applied migrations
(mig/rollback-all CONFIG)

;; rollback the current migration
(mig/rollback-one CONFIG)

;; rollback to the given migration
(mig/rollback-to CONFIG 20230228)
~~~

The `read-disk-migrations` function reads migrations from disk. It returns a
sorted map without information about whether migrations have been applied:

~~~clojure
(mig/read-disk-migrations "migrations")

{1
 {:id 1
  :slug "create users"
  :url-prev #object[java.net.URL "file:/.../migrations/001.create-users.prev.sql"]
  :url-next #object[java.net.URL "file:/.../migrations/001.create-users.next.sql"]}
 2
 {:id 2
  :slug "create profiles"
  :url-prev #object[java.net.URL "file:/.../migrations/foobar/002.create-profiles.prev.sql"]
  :url-next #object[java.net.URL "file:/.../migrations/foobar/002.create-profiles.next.sql"]}
 ...}
~~~

The `make-scope` function accepts a config map and returns a scope map. The
scope map knows everything about the state of migrations, namely: which of them
have been applied, what is the current migration, the table name, the resource
path, and more.

The function `create-migration-files` creates and returns a pair of empty SQL
files. By default, the id is generated from the current date & time, and the
slug is missing:

~~~clojure
(create-migration-files "migrations")

[#object[java.io.File "migrations/20240313120122.prev.sql"]
 #object[java.io.File "migrations/20240313120122.next.sql"]]
~~~

Pass id and slug in options if needed:

~~~clojure
(create-migration-files "migrations" {:id 12345 :slug "Hello migration"})

[#object[java.io.File "migrations/12345.hello-migration.prev.sql"]
 #object[java.io.File "migrations/12345.hello-migration.next.sql"]]
~~~

## Conflicts

On bootstrap, the engine checks migrations for conflicts. A conflict is a
situation when a migration with less id has been applied before a migration with
greater id. Usually it happens when two developers create migrations in parallel
and merge them in a wrong order. For example:

- the latest migration id is 20240312;
- developer A makes a new branch and creates a migration 20240315;
- the next day, developer B opens a new branch with a migration 20240316;
- dev B merges the branch, now we have 20240312, then 20240316;
- dev A merges the branch, and we have 20240312, 20240316, 20240315.

When you try to apply migration 20240315, the engine will check if 20240316 has
already been applied. If yes, an exception pops up saying which migration cause
the problem (in our case, these are 20240316 and 20240315). To recover from the
conflict, rename 20240315 to 20240317.

In other words: this is a conflict:

~~~
id        applied?
20240312  true
20240315  false
20240316  true  ;; applied before 20240315
~~~

And this is a solution:

~~~
id        applied?
20240312  true
20240316  true
20240317  false ;; 20240315 renamed to 20240317
~~~
