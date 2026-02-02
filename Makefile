.PHONY: help build run test clean install docker-chroma

help:
	@echo "Available commands:"
	@echo "  make build         - Build the project"
	@echo "  make run           - Run the application"
	@echo "  make test          - Run tests"
	@echo "  make clean         - Clean build artifacts"
	@echo "  make install       - Install dependencies"
	@echo "  make docker-chroma - Start Chroma in Docker"

build:
	./mvnw clean package

run:
	./mvnw spring-boot:run

test:
	./mvnw test

clean:
	./mvnw clean

install:
	./mvnw clean install

docker-chroma:
	docker run -d -p 8000:8000 --name chroma chromadb/chroma
