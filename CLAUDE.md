# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build              # compile, test, assemble JAR
./gradlew bootRun            # run locally on :9001
./gradlew test               # run all tests
./gradlew test --tests "com.bookstore.bookservice.BookServiceApplicationTests.contextLoads"  # single test
./gradlew bootJar            # produce executable JAR at build/libs/book-service-0.0.1-SNAPSHOT.jar
./gradlew bootBuildImage     # build OCI container image via Spring Boot Buildpacks (no Dockerfile needed)
docker run --rm -p 9001:9001 book-service:0.0.1-SNAPSHOT  # run the container
./gradlew clean              # delete build/
```

No linting or formatting tools are configured. Add Checkstyle or Spotless to `build.gradle` if needed.

## Architecture

Spring Boot 4.0.6 REST microservice, Java 21, Gradle 9.4.1.

- **Entry point:** `src/main/java/com/bookstore/bookservice/BookServiceApplication.java` — `@SpringBootApplication` bootstrap only.
- **Package root:** `com.bookstore.bookservice`
- **Config:** `src/main/resources/application.yaml` — server on port 9001, Tomcat thread/timeout tuning.
- **Tests:** `src/test/java/` — JUnit 5 + Mockito via `spring-boot-starter-test`.

Spring Framework 7.0.7 underlies the stack. No database or ORM is configured — persistence is in-memory (`ConcurrentHashMap`).

### Package layout

```
com.bookstore.bookservice
├── domain/        Book.java (record: isbn, title, author, publisher, publishedDate, price)
├── exception/     BookNotFoundException, BookAlreadyExistsException
├── repository/    BookRepository (interface), InMemoryBookRepository (@Repository)
├── service/       BookService (UpsertResult nested record for PUT upsert)
└── web/           BookController, GlobalExceptionHandler (@RestControllerAdvice, ProblemDetail)
```

### REST API

| Method | Path | Success | Error | Notes |
|--------|------|---------|-------|-------|
| GET | `/books` | 200 `Book[]` | — | |
| POST | `/books` | 201 `Book` | 422 | duplicate ISBN |
| GET | `/books/{isbn}` | 200 `Book` | 404 | |
| PUT | `/books/{isbn}` | 200/201 `Book` | — | 200=updated, 201=created |
| DELETE | `/books/{isbn}` | 204 | — | |

### Repository design

`InMemoryBookRepository` uses two atomic `ConcurrentHashMap` operations to avoid separate existence-check + save calls:

- `saveIfAbsent(Book)` → `putIfAbsent` — returns `true` if inserted, `false` if ISBN already existed (used by `addBookToCatalog`)
- `upsert(Book)` → `put` returning previous value — returns `true` if new entry, `false` if updated (used by `upsertBook`)

### Spring Boot 4 / Spring Framework 7 gotchas

**No `@WebMvcTest`** — the web MVC test slice was not ported. Use instead:
- **Controller unit tests:** `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(advice).build()` with `@ExtendWith(MockitoExtension.class)`
- **Integration tests:** `@SpringBootTest(webEnvironment = MOCK)` + `MockMvcBuilders.webAppContextSetup(context).build()`

**No `TestRestTemplate`** — removed in Spring Boot 4. Use MockMvc with `WebEnvironment.MOCK`.

**`@MockitoBean`** is in `org.springframework.test.context.bean.override.mockito` (Spring Framework 7), not a Spring Boot package.

**Jackson 3.x** changed the package root: `tools.jackson.databind.ObjectMapper` (not `com.fasterxml.jackson.databind`).

**`HttpStatus.UNPROCESSABLE_ENTITY` is `@Deprecated`** — use `HttpStatus.UNPROCESSABLE_CONTENT` and `status().isUnprocessableContent()` per RFC 9110.

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
kubectl run book-service --image=book-service:0.0.1-SNAPSHOT --image-pull-policy=Never --port=9001 --restart=Never
kubectl expose pod book-service --type=NodePort --port=9001
kubectl wait --for=condition=Ready pod/book-service --timeout=60s
kubectl port-forward pod/book-service 9001:9001   # then curl http://localhost:9001/books
```

`--image-pull-policy=Never` tells Kubernetes to use the image already loaded into Minikube rather than pulling from a registry.

On macOS with the Docker driver, Minikube's internal IP is not routable from the host, so `kubectl port-forward` is required (NodePort alone is insufficient).

Teardown: `kubectl delete pod/book-service svc/book-service`
