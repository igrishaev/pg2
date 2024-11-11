
repl: cleanup
	PG_DEBUG=1 lein with-profile +test repl

repl-ssl: cleanup
	PG_DEBUG=1 PG_SSL_PORT=${ssl_port} lein with-profile +test repl

test: cleanup
	lein test

ssl_port ?= $(error Please specify the ssl_port=... argument)

test-ssl: cleanup
	PG_SSL_PORT=${ssl_port} lein test pg.ssl-test

cleanup:
	rm -rf target

build: cleanup
	lein uberjar
	lein install

.PHONY: test
