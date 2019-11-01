.PHONY: test build

test:
	./mvnw clean verify

build:
	docker build -t pitchfork:test . -f Dockerfile
