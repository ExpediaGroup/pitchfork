.PHONY: test build push release

test:
	./mvnw clean verify

build:
	docker build -t vthakurdocker/pitchfork:latest . -f Dockerfile

push:
	docker push vthakurdocker/pitchfork

# build all and release
release: test build push
