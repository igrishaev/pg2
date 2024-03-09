
release:
	lein release

docker-up:
	docker-compose up

docker-down:
	docker-compose down --remove-orphans

docker-rm:
	docker-compose rm --force

docker-psql:
	psql --port 35432 --host localhost -U test test

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

install:
	lein sub with-profile uberjar install

# https://gist.github.com/achesco/b893fb55b90651cf5f4cc803b78e19fd
certs-generate:
	mkdir -p certs
	cd certs && umask u=rw,go= && openssl req -days 3650 -new -text -nodes -subj '/C=US/ST=Test/L=Test/O=Personal/OU=Personal/emailAddress=test@test.com/CN=localhost' -keyout server.key -out server.csr
	cd certs && umask u=rw,go= && openssl req -days 3650 -x509 -text -in server.csr -key server.key -out server.crt
	cd certs && umask u=rw,go= && cp server.crt root.crt
	cd certs && rm server.csr
	cd certs && umask u=rw,go= && openssl req -days 3650 -new -nodes -subj '/C=US/ST=Test/L=Test/O=Personal/OU=Personal/emailAddress=test@test.com/CN=test' -keyout client.key -out client.csr
	cd certs && umask u=rw,go= && openssl x509 -days 3650 -req  -CAcreateserial -in client.csr -CA root.crt -CAkey server.key -out client.crt
	cd certs && rm client.csr

test:
	lein sub install
	PG_MIGRATION_PASS=test lein sub test

.phony: test
