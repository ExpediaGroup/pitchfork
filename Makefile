.PHONY: test build

test:
	./mvnw clean verify

build:
	docker build -t expediagroup/pitchfork:test . -f Dockerfile
