
repl: cleanup
	PG_DEBUG=1 lein with-profile +test repl

test: cleanup
	lein with-profile +test test

cleanup:
	rm -rf target

build: cleanup
	lein uberjar
	lein install

.PHONY: test
