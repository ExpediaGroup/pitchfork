.PHONY: test build release

test:
	./mvnw clean verify

build:
	docker build -t expediagroup/pitchfork:test . -f Dockerfile

# build all and release
release: test build #docker_login docker_tag docker_push
