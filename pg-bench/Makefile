
java_home ?= $(error Please specify the java_home=... argument)

main: repl

repl:
	JAVA_HOME=${java_home} lein repl

bench:
	JAVA_HOME=${java_home} lein run

type ?= $(error Please specify the type=... argument: pg | jdbc)

server:
	lein run -m pg.server ${type}
