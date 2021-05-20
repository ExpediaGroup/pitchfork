.PHONY: test build

test:
	./mvnw clean verify

build:
	docker build -t worldtiki/pitchfork:test123 . -f Dockerfile
