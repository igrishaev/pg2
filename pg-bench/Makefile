
main: repl

repl:
	lein repl

java_home ?= $(error Please specify the java_home=... argument)

bench:
	export JAVA_BIN=${java_home}/bin/java && lein run
