
## 0.1.7-SNAPSHOT

- migrations
  - fix file names in readme
  - fix file parser (dot vs hyphen)
  - rename files (in jar as well)

## 0.1.6

- msg package split on msg.server and msg.client
- ClientMessage and ServerMessage interfaces
- PGError and PGErrorResponse extend RuntimeException
- fixed offset/limit bug when parsing binary json
- make CodecParams class immutable
- ObjectMapper support in:
  - connection,
  - codec params,
  - pg.json ns,
  - encode/decode-bin/text,
  - JSON middleware

## 0.1.5

- migrations
- toc depth
- honey queries function

## 0.1.4

- pg-honey helpers
- pg-honey demo and reamde

## 0.1.3

- Next.JDBC wrapper
- more docs
- with-tx macro refactored
- error! function added
- msCancelTimeout renamed
- requireNonNull messages added
- connection isClosed refactored
- org.pg.proto package
- int to float encoding support
- oids type hints bug fixed
- concurrency tests

## 0.1.2

- new benchmarks
- Makefile java path
- LazyMap and LazyVector
- prg.pg.clojure package
- reducers refactored

## 0.1.1

- plan/reduce benchmarks
- reducer init() method accepts keys
- matrix header row added
- with-tx reflection warning removed

## 0.1.0

- initial release
