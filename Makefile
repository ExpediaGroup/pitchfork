.PHONY: test package build push release

MAVEN := ./mvnw

test:
	${MAVEN} clean verify

package:
	${MAVEN} clean package

build:
	docker build -t vthakurdocker/pitchfork:latest -t vthakurdocker/pitchfork:1.24 . -f Dockerfile

push:
	docker push vthakurdocker/pitchfork

# build all and release
release: package build push
