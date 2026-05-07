# book-service

A Spring Boot REST microservice serving as the book catalog backend.

- **Spring Boot** 4.0.6 / **Spring Framework** 7.0.7
- **Java** 21
- **Gradle** 9.4.1

## Endpoints

| Method | Path | Response |
|--------|------|----------|
| GET | `/` | `Welcome to the book catalog!` |

## Run locally

```bash
./gradlew bootRun        # starts on http://localhost:8080
./gradlew test           # run all tests
./gradlew build          # compile, test, assemble JAR
```

## Run in Docker

```bash
./gradlew bootBuildImage
docker run --rm -p 8080:8080 book-service:0.0.1-SNAPSHOT
```

Images are built with [Paketo Buildpacks](https://paketo.io/) — no Dockerfile needed. The builder is chosen automatically by CPU architecture (ARM64 or x86-64).

## Run on Minikube (no manifests)

```bash
minikube start
./gradlew bootBuildImage
minikube image load book-service:0.0.1-SNAPSHOT
kubectl run book-service --image=book-service:0.0.1-SNAPSHOT --image-pull-policy=Never --port=8080 --restart=Never
kubectl expose pod book-service --type=NodePort --port=8080
kubectl wait --for=condition=Ready pod/book-service --timeout=60s
kubectl port-forward pod/book-service 8080:8080
# curl http://localhost:8080
```

Teardown:

```bash
kubectl delete pod/book-service svc/book-service
```
