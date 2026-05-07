# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build              # compile, test, assemble JAR
./gradlew bootRun            # run locally on :8080
./gradlew test               # run all tests
./gradlew test --tests "com.bookstore.bookservice.BookServiceApplicationTests.contextLoads"  # single test
./gradlew bootJar            # produce executable JAR at build/libs/book-service-0.0.1-SNAPSHOT.jar
./gradlew bootBuildImage     # build OCI container image via Spring Boot Buildpacks (no Dockerfile needed)
docker run --rm -p 8080:8080 book-service:0.0.1-SNAPSHOT  # run the container
./gradlew clean              # delete build/
```

No linting or formatting tools are configured. Add Checkstyle or Spotless to `build.gradle` if needed.

## Architecture

Spring Boot 4.0.6 REST microservice, Java 21, Gradle 9.4.1.

- **Entry point:** `src/main/java/com/bookstore/bookservice/BookServiceApplication.java` — `@SpringBootApplication` bootstrap only.
- **Package root:** `com.bookstore.bookservice`
- **Config:** `src/main/resources/application.yaml` — currently sets only `spring.application.name: book-service`.
- **Tests:** `src/test/java/` — JUnit 5 via `spring-boot-starter-webmvc-test`; currently only a context-load smoke test.
- **Controllers:** `HomeController` handles `GET /` and returns `"Welcome to the book catalog!"`.

Spring Framework 7.0.7 underlies the stack. No database or ORM dependency is configured.

## Container image

Images are built via `bootBuildImage` (Paketo Buildpacks) — there is no Dockerfile. The builder is selected by architecture in `build.gradle`:

- **ARM64:** `dashaun/builder:tiny` — required because other ARM64 builders (e.g. `ghcr.io/thomasvitale/java-builder-arm64`) run the lifecycle as root, which CNB rejects.
- **x86-64:** `paketobuildpacks/builder-jammy-tiny`

Buildpacks sets the image creation date to epoch by default (shows as "46 years ago"). Add `createdDate = 'now'` to the `bootBuildImage` block in `build.gradle` to use the current timestamp.

## Minikube (no manifests)

Deploy and test imperatively — no YAML files needed:

```bash
minikube start
./gradlew bootBuildImage
minikube image load book-service:0.0.1-SNAPSHOT
kubectl run book-service --image=book-service:0.0.1-SNAPSHOT --image-pull-policy=Never --port=8080 --restart=Never
kubectl expose pod book-service --type=NodePort --port=8080
kubectl wait --for=condition=Ready pod/book-service --timeout=60s
kubectl port-forward pod/book-service 8080:8080   # then curl http://localhost:8080
```

`--image-pull-policy=Never` tells Kubernetes to use the image already loaded into Minikube rather than pulling from a registry.

On macOS with the Docker driver, Minikube's internal IP is not routable from the host, so `kubectl port-forward` is required (NodePort alone is insufficient).

Teardown: `kubectl delete pod/book-service svc/book-service`
