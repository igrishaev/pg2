repl: cleanup
	PG_DEBUG=1 lein with-profile +test repl

test: cleanup
	lein test

cleanup:
	rm -rf target

.PHONY: test
