.PHONY: test build release

test:
	./mvnw clean verify

build:
	docker build -t expediagroup/pitchfork:test . -f Dockerfile
	docker tag pitchfork vthakurdocker/pitchfork

# build all and release
release: test build #docker_login docker_tag docker_push
