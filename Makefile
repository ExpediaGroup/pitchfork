.PHONY: test build tag push login release

test:
	./mvnw clean verify

build:
	docker build -t vthakur/pitchfork:latest . -f Dockerfile

tag:
	docker tag pitchfork vthakur/pitchfork:latest

push:
	docker push vthakur/pitchfork

login:
	echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

# build all and release
release: test build login tag push
