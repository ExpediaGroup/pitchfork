.PHONY: test build release

test:
	./mvnw clean verify

build:
	docker build -t vthakur/pitchfork:latest . -f Dockerfile

tag:
	docker tag pitchfork vthakur/pitchfork:latest

push:
	docker push $QUALIFIED_DOCKER_IMAGE_NAME

# build all and release
release: test build tag push
