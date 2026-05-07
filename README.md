# book-service

A Spring Boot REST microservice serving as the book catalog backend.

- **Spring Boot** 4.0.6 / **Spring Framework** 7.0.7
- **Java** 21
- **Gradle** 9.4.1
- **Persistence** in-memory (`ConcurrentHashMap`) — no database required

## Book model

| Field | Type | Required |
|-------|------|----------|
| `isbn` | String | yes |
| `title` | String | yes |
| `author` | String | yes |
| `publisher` | String | no |
| `publishedDate` | LocalDate (`yyyy-MM-dd`) | no |
| `price` | Decimal ≥ 0 | yes |

## Endpoints

| Method | Path | Success | Error | Notes |
|--------|------|---------|-------|-------|
| GET | `/books` | 200 `Book[]` | — | All books |
| POST | `/books` | 201 `Book` | 422 | Duplicate ISBN |
| GET | `/books/{isbn}` | 200 `Book` | 404 | |
| PUT | `/books/{isbn}` | 200/201 `Book` | — | 200=updated, 201=created |
| DELETE | `/books/{isbn}` | 204 | — | |

Error responses follow [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`).

## Run locally

```bash
./gradlew bootRun        # starts on http://localhost:9001
./gradlew test           # run all tests
./gradlew build          # compile, test, assemble JAR
```

## Run in Docker

```bash
./gradlew bootBuildImage
docker run --rm -p 9001:9001 book-service:0.0.1-SNAPSHOT
```

Images are built with [Paketo Buildpacks](https://paketo.io/) — no Dockerfile needed. The builder is chosen automatically by CPU architecture (ARM64 or x86-64).

## Run on Minikube (no manifests)

```bash
minikube start
./gradlew bootBuildImage
minikube image load book-service:0.0.1-SNAPSHOT
kubectl run book-service --image=book-service:0.0.1-SNAPSHOT --image-pull-policy=Never --port=9001 --restart=Never
kubectl expose pod book-service --type=NodePort --port=9001
kubectl wait --for=condition=Ready pod/book-service --timeout=60s
kubectl port-forward pod/book-service 9001:9001
```

Then test:

```bash
curl http://localhost:9001/books
curl -X POST http://localhost:9001/books \
  -H 'Content-Type: application/json' \
  -d '{"isbn":"978-1491950357","title":"Cloud Native Java","author":"Josh Long","price":49.99}'
curl http://localhost:9001/books/978-1491950357
curl -X DELETE http://localhost:9001/books/978-1491950357
```

Teardown:

```bash
kubectl delete pod/book-service svc/book-service
```
