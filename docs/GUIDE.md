# Job portal backend — learning guide

This is a small Spring Boot API written so you can read it and see how the current pieces fit together. It is the backend for a job board: people register, log in, and publish job posts. Anyone can read the posts. Only the account that created a post can change or delete it.

The stack is the current stable line as of October 2026:

| Piece | Version | What it is |
| --- | --- | --- |
| Java | 25 LTS | Language. Records, sealed classes, pattern matching, virtual threads. |
| Spring Boot | 4.1.2 | Starts the app and wires libraries from the classpath. |
| Spring Framework | 7.0 | MVC, dependency injection, `@Transactional`. |
| Spring Security | 7.1 | Filter chain, password hashing. |
| Spring Data JPA | 2026.0 | Repository interfaces that become SQL. |
| Hibernate | 7.4 | The JPA implementation that talks to PostgreSQL. |
| Jackson | 3 | JSON. Packages start with `tools.jackson`, not `com.fasterxml.jackson.databind`. |
| Tomcat | 11 | The embedded HTTP server. |
| jjwt | 0.13 | Creates and checks JWTs. |
| springdoc | 3.2 | OpenAPI / Swagger UI. |
| Testcontainers | 2.0 | Real PostgreSQL in Docker during tests. |

Parent versions come from `spring-boot-starter-parent` in `pom.xml`. We only pin springdoc and jjwt, because Boot does not manage those.

## What changed

The previous code was Spring Boot 3.3.5 on Java 21. Behavior of the happy path is the same: the same URLs, the same job JSON fields, a raw JWT from `POST /login`, and the same delete sentence. A few bugs and all of the outdated APIs were removed.

### Libraries and style

- Java 25 and Spring Boot 4.1. `spring-boot-starter-web` is now `spring-boot-starter-webmvc`. `spring-boot-starter-aop` is now `spring-boot-starter-aspectj`.
- Jackson 3. The `ObjectMapper` you inject for HTTP JSON is a `JsonMapper` (`tools.jackson.databind.json.JsonMapper`).
- jjwt 0.13. `parserBuilder()`, `setSubject()`, and `signWith(key, SignatureAlgorithm.HS256)` are gone. The code uses `Jwts.builder().subject(...)`, `signWith(key)`, and `Jwts.parser().verifyWith(key)`.
- Lombok and the unversioned `org.jetbrains:annotations` dependency are gone. DTOs are records. Entities have ordinary getters and setters, because `@Data` on a JPA entity generates `equals`/`hashCode` from every column, which fights Hibernate.
- Field `@Autowired` is gone in production code. Dependencies are constructor parameters. Spring calls that constructor.
- `javax.persistence` and `javax.servlet` were already `jakarta.*` in the old code. `javax.crypto` is still correct: that package belongs to the JDK, not to old Java EE.
- Virtual threads are on (`spring.threads.virtual.enabled=true`). A test hits real Tomcat and checks `Thread.isVirtual()`.
- Actuator (`/actuator/health`, `/actuator/info`) and springdoc (`/swagger-ui.html`, `/v3/api-docs`) are on the classpath.
- Tests are `@SpringBootTest` + MockMvc + Testcontainers Postgres. `@AutoConfigureMockMvc` is required: Boot 4 no longer puts a `MockMvc` bean in the context just because you wrote `@SpringBootTest`.

### Bugs that were fixed

- The JWT signing key was generated again on every startup and printed to the console. Tokens died at every restart, and the key leaked into logs. The key is now `app.jwt.secret` (override it with `JWT_SECRET`). It is not logged.
- `GET /jobPost/{id}` returned `200` and an empty `JobPost` when the id did not exist. The README already promised `404`. It is `404` now.
- Update and delete compared the `addedBy` field **in the request body** with the token. Any logged-in user could overwrite another person's job by sending their own username. The service now loads the row and compares the stored owner. The body value is ignored.
- `POST /register` returned the BCrypt hash. The response is `{userId, username}` only.
- `POST /login` had an `else` that returned `"Invalid username or password"`, but `authenticate()` throws before that line. Failures now hit that same sentence, with status `401` and `Content-Type: text/plain`.
- An aspect rewrote a negative id into a positive one (`-3` became `3`) and continued. That hid bad input. Negative ids are `400` from `@Positive`.
- Two separate `BCryptPasswordEncoder` instances (one in security config, one in the controller) are one shared bean.
- `open-in-view` was on, so a lazy collection could be loaded during JSON rendering, after the service had finished. It is off. Repositories fetch the tech stack inside the transaction.

### API notes for the frontend

Keep these if you change the Next.js app:

1. `POST /login` success body is the raw JWT (`aaa.bbb.ccc`), not JSON. The home page stores `response.text()` unless the text is exactly `Invalid username or password`.
2. `POST /jobPost` success status is **200**. `AddJobButton` throws when `response.status !== 200`, so `201` would look like a failure after a successful insert.
3. `POST /register` is `201` and the JSON has no `password`. The page only checks `response.ok` and parses JSON, so this still works.
4. Other errors are Problem Details (`application/problem+json`): `status`, `title`, `detail`, `instance`, and sometimes `errors`.
5. `addedBy` on a write is taken from the token's `sub` claim. Sending a different `addedBy` does not change the owner and does not grant edit rights.
6. `GET /loadData` was already commented out. It is deleted. The UI never called it.
7. Search is still case-sensitive, same as the old `Containing` query.

`requiredExperience` may be a JSON number or a numeric string (`"2"`). The React inputs store strings. Jackson is configured to coerce those strings to integers. A non-numeric string is `400`.

## How a request moves

`POST /jobPost` with `Authorization: Bearer <jwt>`:

1. Tomcat accepts the socket on a virtual thread.
2. `JwtAuthenticationFilter` reads the header, checks the signature and expiry, loads the user, and stores a `UserPrincipal` in the `SecurityContext`.
3. The security chain checks the route. This one is not public, so an empty context would be `401`. A valid token continues.
4. `JobController.addJob` runs. `@Valid` checks the record. `@AuthenticationPrincipal` is the principal from step 2.
5. `JobService.addJob` opens a transaction, builds a `JobPost`, sets `addedBy` from the principal, and `save`s it.
6. `JobMapper` copies the entity into a `JobResponse` record before the transaction ends (the tech-stack list is lazy).
7. Jackson writes the record as JSON. The component names are the JSON names.

A failure in step 4 or 5 is caught by `GlobalExceptionHandler` and written as Problem Details. A missing token never reaches the controller: the chain's authentication entry point writes a Problem Detail itself, because that callback is not a controller method.

## Project structure

Everything lives under `org.example.jobportalappbackend`, which is also the package `@SpringBootApplication` scans.

| Package | Role | Talks to |
| --- | --- | --- |
| `controller` | HTTP only. Status codes, `@Valid`, path variables. | services |
| `service` | Use cases and transactions. | repositories, security helpers |
| `repository` | Interfaces. Spring Data writes the implementation. | the database, via Hibernate |
| `model` | Entities. One instance is one row (plus the tech-stack collection). | nothing HTTP-related |
| `dto` | Records that enter or leave HTTP. | nothing |
| `mapper` | Entity to response record. A static method, not a bean, because it has no dependencies. | entities and DTOs |
| `security` | Passwords, JWT, the servlet filter. | repositories |
| `config` | Beans that are settings: the filter chain, CORS, Jackson, OpenAPI. | properties records |
| `exception` | Known failures and the advice that maps them to HTTP. | nothing |
| `aop` | Logs and timings around `JobService`. | the service proxy |

`JobPortalApplication` is the `main` method. `@SpringBootApplication` means three things: this class may declare `@Bean` methods, Boot's auto-configuration may start Tomcat/JPA/Security/Jackson because those starters are on the classpath, and stereotype classes in this package are picked up.

## Dependency injection

A service does not call `new JobRepository()`. It lists `JobRepository` as a constructor parameter. Spring has one repository instance (a bean) and passes it in. That is constructor injection.

Why this shape:

- The field can be `final`. The object always has its dependencies.
- Tests can pass a fake without Spring, as `JwtServiceTest` does.
- You can see the dependencies by reading the constructor.

`@Service`, `@Component`, and `@RestController` mark classes Spring should construct. `@Bean` methods in `@Configuration` classes do the same for objects you build yourself (`PasswordEncoder`, the `SecurityFilterChain`).

`@ConfigurationPropertiesScan` plus `@ConfigurationProperties` on a record binds `app.jwt.secret` to `JwtProperties.secret`. Boot calls the record constructor. The compact constructor rejects a secret shorter than 32 bytes, which is the minimum HMAC-SHA256 key jjwt will use.

## JPA and entities

`User` and `JobPost` are `@Entity` classes. They are not records: Hibernate needs a no-argument constructor and must be able to change fields after it loads a row.

Annotations worth knowing on `JobPost`:

- `@Id` and `@GeneratedValue(strategy = IDENTITY)` — PostgreSQL assigns `post_id` on insert. This matches the table the old app created. Switching to a sequence would be a different schema.
- `@Column(name = "...")` — Java name and column name differ (`postProfile` / `post_profile`). Keeping the old names means an existing database still lines up.
- `@ElementCollection` — `postTechStack` is a list of strings in table `job_post_tech_stack`, not a second entity. There is no `Technology` class because a technology here is only a label.

`spring.jpa.hibernate.ddl-auto=update` alters tables to match the entities when the server starts. That is convenient for learning and risky on a database you cannot rebuild. Tests use `create-drop` instead, in `src/test/resources/application-test.properties`, which is loaded by `@ActiveProfiles("test")`.

`spring.jpa.open-in-view=false` closes the Hibernate session when the service transaction ends. If a controller then touched `job.getPostTechStack()` and nobody had loaded it yet, you would get `LazyInitializationException`. That is the point: the mistake shows up immediately. `JobRepository` methods carry `@EntityGraph(attributePaths = "postTechStack")` so the list is loaded with the job, in one query, instead of one extra query per job (the N+1 pattern).

`@Transactional` on `JobService` opens the session. The class-level annotation is `readOnly = true`. A write method declares `@Transactional` again. Spring does not merge the two; the method annotation replaces the class one, so the write runs in a read-write transaction.

Repository method names are queries. `UserRepository.findByUsername` becomes a `WHERE user_name = ?`. The search method uses a `@Query` text block instead, because the condition is easier to read as JPQL. `:keyword` is a bind parameter. The user's text cannot change the SQL.

When a job is updated, the tech-stack list is cleared and refilled in place. Hibernate is tracking that collection instance. Replacing it with a brand-new `ArrayList` can leave the old join-table rows behind.

Entities are not returned from controllers. A future change that serializes `User` still cannot leak the hash: the password field is `@JsonIgnore`, and `toString` omits it.

## DTOs and records

A record is an immutable data carrier. `JobResponse` gives you a constructor, accessors, `equals`, and `hashCode`. Jackson 3 uses the component names as JSON properties, so the frontend keeps reading `postProfile` and `postTechStack`.

`JobWriteRequest` is the body of create and update. `postId` is null on create (the database assigns it) and required on update. `addedBy` is on the record so the existing frontend payload still deserializes, and the service ignores it.

`LoginRequest` and `RegisterRequest` are separate types. A login is not a user row, and a response must not be an entity.

`UserResponse` has two components. There is no password component, so it cannot be serialized by accident.

## Validation

Constraints live on the record components: `@NotBlank`, `@Size`, `@NotNull`, `@Min`. They do nothing until something asks for them. `@Valid` on the controller parameter is that request. A failure throws `MethodArgumentNotValidException` before the controller method runs.

`JobController` is also `@Validated`, which turns `@Positive` on a path variable into a real check. `GET /jobPost/0` is `400`, not a database lookup.

Boot turns problem-details support on with `spring.mvc.problemdetails.enabled=true` and registers its own `@ControllerAdvice` (`ProblemDetailsExceptionHandler`). That advice handles validation with the generic detail "Invalid request content." and no field list. `@ControllerAdvice` beans are tried in `@Order`, and the first match wins. `GlobalExceptionHandler` is `@Order(HIGHEST_PRECEDENCE)` so its handler runs instead and adds:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Request validation failed",
  "errors": {
    "postProfile": "Job title is required",
    "requiredExperience": "Required experience cannot be negative"
  }
}
```

`errors` is not a standard Problem Details field. The spec allows extra members. `ProblemDetail.setProperty` adds one, and Spring's Jackson mixin flattens those properties onto the JSON object.

BCrypt only uses the first 72 bytes of a password. `@Size(max = 72)` on registration rejects longer values instead of silently truncating them.

## Exception handling

Known domain failures are a sealed hierarchy:

```text
PortalException
  JobNotFoundException          404
  ForbiddenJobAccessException   403
  DuplicateUsernameException    409
```

`sealed` plus `permits` means those are the only subclasses. The handler uses a `switch` on the type. If you add a subclass and forget a branch, the code does not compile. That is pattern matching doing the job of a chain of `instanceof` checks.

`ProblemDetail` is Spring's RFC 9457 type. Returning it from an `@ExceptionHandler` sets `Content-Type: application/problem+json`.

Login does not use that shape. The frontend stores any login body that is not exactly `Invalid username or password`. A JSON problem document would be saved as the token. `GlobalExceptionHandler` returns plain text for `/login`.

The security entry point (no token on a protected route) is not an `@ExceptionHandler`. `SecurityConfig` writes a Problem Detail with the `JsonMapper` directly.

## Security and JWT

There is no server session. `SessionCreationPolicy.STATELESS` says so. Each request carries its own token. CSRF protection is off because CSRF defends cookie sessions: a browser would attach the cookie automatically. This API does not use a session cookie. The browser must set `Authorization` on purpose. Form login and HTTP Basic are disabled so those mechanisms cannot appear beside the JWT filter.

The chain, in the order it is configured:

1. CORS, using the origins from `WebConfig` / `app.cors.allowed-origins`. `allowCredentials(true)` requires a specific origin. `*` is illegal in that mode.
2. Authorization rules. Public: job reads, `/login`, `/register`, Swagger, `/actuator/health`, `/actuator/info`, and `/error`. Everything else needs an authenticated principal.
3. `JwtAuthenticationFilter` runs before Spring Security's username/password filter. The header is read with a pattern `switch`: a missing header and a non-Bearer header are the same case (stay anonymous). A bad signature does not abort the request. Public GETs still work if the client sends garbage. Protected routes then fail with `401`.
4. On success the filter stores a `UsernamePasswordAuthenticationToken` whose principal is a `UserPrincipal`.

`UserPrincipal` implements `UserDetails`. It copies the username and the hash out of the entity so the security context does not keep a Hibernate object. `getAuthorities()` returns `ROLE_USER`. `hasRole("USER")` looks for that `ROLE_` prefix. No route uses the role yet; rules use `authenticated()`, and ownership is a row check in the service, not a role.

Passwords are BCrypt with cost 12, the same cost the old app used. `matches` reads the cost from the stored hash (`$2a$12$...`), so rows created earlier still verify. One `PasswordEncoder` bean is used both to hash at registration and to check at login.

`DaoAuthenticationProvider` in Spring Security 7 takes the `UserDetailsService` in its constructor. The no-arg constructor was removed. `ProviderManager` is the `AuthenticationManager` that `POST /login` calls. If the password does not match, it throws `BadCredentialsException`. There is no `else`.

A token is `header.payload.signature`. We set `sub` (the username), `iat`, and `exp` (30 minutes, matching the frontend timer). jjwt picks HMAC-SHA256 from the key length. The parser uses `verifyWith`, which checks the signature, then we check `exp` ourselves as well.

The controller never slices the header. It asks for `@AuthenticationPrincipal UserPrincipal`. That only exists because the filter ran.

## Configuration and profiles

`src/main/resources/application.properties` is the default configuration. Every key there has a comment. The important ones:

- `spring.threads.virtual.enabled=true` — Tomcat handles the request on a virtual thread. The app waits on the database more than it burns CPU, which is the case virtual threads are for. Java 25 is past the Java 24 baseline Boot recommends for this flag. Pool-size properties stop mattering, because virtual threads are not taken from a fixed pool. The web server still has non-daemon threads, so the process stays up. `spring.main.keep-alive` matters for apps whose only threads are virtual (a scheduler and nothing else). This one is a web server, so it is left off.
- `spring.datasource.*` — local Postgres. Environment variables override the defaults. The password `12345` is the old learning default, not a secret you should deploy.
- `server.error.include-stacktrace=never` — stack traces are for logs, not for clients.

`src/test/resources/application-test.properties` is the `test` profile. It turns SQL logging down and uses `create-drop`. `@ServiceConnection` on the Testcontainers Postgres instance replaces the JDBC URL, username, and password. You do not copy the container's mapped port into a property by hand.

## AOP

`LoggingAspect` and `PerformanceMonitorAspect` are `@Aspect` beans. Boot enables AspectJ proxying because `spring-boot-starter-aspectj` is present. You do not call these methods.

The pointcut `execution(* org.example.jobportalappbackend.service.JobService.*(..))` means: any return type, any method of `JobService`, any arguments. It matches calls that go through the Spring proxy. A private method calling another method on `this` does not. That is a limit of Spring AOP, not a missed annotation.

- `@Before` runs before the service method. `JoinPoint` describes the call.
- `@After` runs after it returns or throws. The logging aspect uses it only for `getJobPosts`, so you can see a narrow pointcut next to a wide one.
- `@Around` can time the call. `proceed()` continues to the next advice and then the real method. The timer aspect is `@Order(1)` and the logger is `@Order(2)`. Lower order is the outer wrapper, so the timer includes the log advice.

The old validation aspect that rewrote negative ids was removed. Input rules belong on the DTO and the controller, where a failure becomes a `400` instead of a silent change.

## Actuator and OpenAPI

Actuator is the production-shaped health check. Only `health` and `info` are exposed (`management.endpoints.web.exposure.include`). Health details (which would include database coordinates) are `when_authorized`, so an anonymous caller sees `{"status":"UP"}` and not the JDBC URL. `/actuator/info` shows the `info.app.*` properties because `management.info.env.enabled=true`.

springdoc reads the controllers and the `OpenAPI` bean in `OpenApiConfig`. `@Operation` and `@SecurityRequirement` add text to that document. They do not change routing. The bearer scheme is documented so Swagger UI has an Authorize box. You paste the raw JWT there, without the word `Bearer`.

## Testing

`JwtServiceTest` does not start Spring. It constructs `JwtService` with a `JwtProperties` record and checks the token round-trip, a wrong key, and a short secret. That is the fast test.

`JobPortalApiTests` is the full path:

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` starts the real application and a real Tomcat. `RANDOM_PORT` avoids clashing with a server you already have on 8080.
- `@AutoConfigureMockMvc` builds a `MockMvc` that runs the filter chain, controllers, and services in that context. It does not open a socket, and it does not run on a Tomcat thread.
- `@Testcontainers` and `@Container` start `postgres:17-alpine` once per class (the field is `static`).
- `@ServiceConnection` points Spring's datasource at that container.
- `@ActiveProfiles("test")` loads the test properties.

The tests register two users, create a job with `requiredExperience` as a string and a spoofed `addedBy`, then check the list, the search, a forbidden edit, a real edit that keeps the original owner, a forbidden delete, the exact delete sentence, and the `404` problem body. They also check plain-text login failure, duplicate usernames, validation errors, Swagger, health, and that `/loadData` and `/actuator/beans` are not available.

`tomcatHandlesRequestsOnVirtualThreads` calls the real port with `HttpClient` and a test `Filter` records `Thread.currentThread().isVirtual()`. MockMvc cannot show this, because it calls the controller on the test thread.

Docker has to be running for `./mvnw verify`. The unit test alone does not need it, but Surefire runs both.

## How to run and build

From the repository root, with Java 25:

```bash
java -version
./mvnw verify
./mvnw spring-boot:run
```

`./mvnw` downloads the Maven version pinned in `.mvn/wrapper/maven-wrapper.properties`. You do not install Maven yourself.

The runnable jar is `target/JobPortalAppBackend-0.0.1-SNAPSHOT.jar` after `./mvnw package`.

See the README for the database container command and the environment variables.

## Glossary

**Advice** — code an aspect runs before, after, or around a matched method.

**AOP** — aspect-oriented programming. A way to run the same extra behavior (logging, timing, transactions) around many methods without calling it from each one.

**Actuator** — Spring Boot's built-in operations endpoints, such as health.

**Bean** — an object Spring constructs and keeps. Most classes annotated `@Service` or `@Component` are beans.

**Bean Validation** — the `@NotBlank` / `@Min` annotations and the engine that checks them. Jakarta Validation is the API. Hibernate Validator is the implementation on the classpath.

**BOM** — bill of materials. The parent POM's list of library versions that are known to work together.

**CSRF** — cross-site request forgery. A browser attaching a cookie to a request the user did not mean to send. Not the threat model of a bearer-token API.

**CORS** — a browser rule about which web origin may call this API. It is not authentication.

**DTO** — data transfer object. The records that cross the HTTP boundary. Not a table.

**Dependency injection** — passing an object's collaborators in from outside, instead of constructing them inside.

**Entity** — a class Hibernate maps to a table.

**Entity graph** — a hint to load a relationship or collection in the same query as the parent.

**Filter chain** — the ordered servlet filters Spring Security runs before your controller.

**HMAC** — a signature that uses a shared secret. HS256 is HMAC with SHA-256. Anyone who has the secret can mint tokens, which is why `JWT_SECRET` must not stay at the dev default.

**IoC container** — the Spring application context. It creates beans and injects them. Inversion of control means the framework calls your code, not the other way around.

**JPA** — Jakarta Persistence. The annotations (`@Entity`, `@Id`) and the `EntityManager` idea. Hibernate is one implementation.

**JWT** — JSON Web Token. A signed string with three Base64url sections. We use it as a bearer token. The payload is readable by anyone; the signature is what makes it trustworthy.

**Lazy loading** — Hibernate does not load a collection until something reads it. That read must happen while a session is open.

**N+1** — one query for the parents, then one more query per parent for a collection. An entity graph avoids it here.

**Open-in-view** — a Boot feature that keeps the Hibernate session open until the HTTP response is written. Convenient, and it hides lazy-loading mistakes. This app turns it off.

**OpenAPI** — a machine-readable description of the HTTP API. Swagger UI renders it.

**Pointcut** — the expression that picks which methods an aspect applies to.

**Problem Details** — RFC 9457. A small JSON error shape: `type`, `title`, `status`, `detail`, `instance`, plus any extra members you add.

**Profile** — a named set of properties. `test` is active during `@SpringBootTest`. The main file is always loaded; the profile file overrides keys.

**Record** — a compact immutable class. Good for DTOs. A poor fit for JPA entities.

**Repository** — a Spring Data interface. You declare methods. Spring generates the implementation.

**Sealed class** — a class that lists its only allowed subclasses. Used here so the error `switch` stays exhaustive.

**Starter** — a Spring Boot dependency that pulls in a stack (web, security, JPA) and turns on auto-configuration.

**Transaction** — a unit of database work that commits or rolls back together. `@Transactional` starts one.

**Virtual thread** — a cheap thread the JVM schedules on a small set of carrier (platform) threads. Useful when the work is waiting on I/O. Not a way to make CPU-heavy code faster.
