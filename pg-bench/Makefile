
main: repl

repl:
	export JAVA_BIN=${java_home}/bin/java && lein repl

java_home ?= $(error Please specify the java_home=... argument)

bench:
	export JAVA_BIN=${java_home}/bin/java && lein run

type ?= $(error Please specify the type=... argument: pg | jdbc)

server:
	lein run -m pg.server ${type}
