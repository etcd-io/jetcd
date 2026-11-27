# Development Guidelines

## Code Style

### Imports

- **Avoid fully qualified class names** except for the `io.etcd.jetcd.api` package
  - Use imports for all other classes
  - Exception: `io.etcd.jetcd.api.*` classes should be fully qualified to distinguish generated proto classes from application code

- **Avoid star imports** (`import java.util.*`)
  - Always use explicit imports for better clarity and to avoid naming conflicts

- **Avoid static imports**
  - Static imports can make code harder to understand by obscuring the source of methods/constants
  - Exception: Test assertions may use static imports where conventional

### Lambda Expressions

- **Prefer method references over inline lambdas** when possible
  - ✅ `stream.map(String::toLowerCase)`
  - ❌ `stream.map(s -> s.toLowerCase())`
  - Use inline lambdas only when the logic is non-trivial or doesn't map to an existing method

## Dependencies

### Preferred Libraries

- **Use Vert.x for async/concurrency** instead of manual executor services
  - Leverage Vert.x's event loop and async primitives
  - Avoid creating custom thread pools or executor services

- **Use Failsafe for retry logic and resilience patterns**
  - Don't reinvent the wheel with custom retry mechanisms
  - Failsafe provides battle-tested retry policies, circuit breakers, and fallbacks

### Libraries to Avoid

- **Avoid Guava** where possible
  - Prefer standard Java APIs and collections
  - Use Java's built-in functional interfaces and utilities

- **Avoid grpc-java** (io.grpc)
  - This project uses Vert.x gRPC, not grpc-java
  - Use `io.vertx.grpc.client.GrpcClient` for gRPC operations
  - Proto stubs are generated with Vert.x gRPC plugin

## Public API Design

- **Public methods should use standard Java APIs**
  - Avoid exposing library-specific types in public interfaces
  - ❌ `public io.vertx.core.Future<Response> getData()`
  - ✅ `public CompletableFuture<Response> getData()`
  - Internal implementation can use Vert.x, but convert to standard types at API boundaries

- **Keep implementation details internal**
  - Use standard `CompletableFuture` for async operations in public APIs
  - Convert between Vert.x futures and `CompletableFuture` as needed
  - This maintains flexibility to change implementation without breaking consumers

## Testing

### Running Tests

```bash
./gradlew test                    # All tests
./gradlew :jetcd-core:test        # Core module only
./gradlew test --tests "*.KVTest" # Specific test class
```

### Using EtcdClusterExtension

Tests use `EtcdClusterExtension` to spin up an etcd cluster via Testcontainers:

```java
import io.etcd.jetcd.test.EtcdClusterExtension;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class MyTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(3)
        .build();

    private Client client;

    @BeforeEach
    void setUp() {
        client = TestUtil.client(cluster).build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testSomething() throws Exception {
        KV kv = client.getKVClient();
        // ...
    }
}
```

### Key Patterns

- Use `TestUtil.client(cluster)` to create a `ClientBuilder` with the cluster's endpoint resolver
- Use `TestUtil.bytesOf("string")` for `ByteSequence` creation
- Use `TestUtil.randomByteSequence()` for unique keys
- Always close clients in `@AfterEach` or `@AfterAll`
- Set `@Timeout` to prevent hanging tests

### Test Tags

Tests are tagged for selective execution:

```bash
./gradlew test -PincludeTags=kv      # KV tests only
./gradlew test -PincludeTags=watch   # Watch tests only
```

## Quality Checks

**All checks must pass before committing.**

```bash
# Run linters and checks
./gradlew check

# Or run specific checks
./gradlew spotlessCheck   # Code formatting
./gradlew pmdMain         # Static analysis
./gradlew compileJava     # Compilation
```

Fix formatting issues automatically:

```bash
./gradlew spotlessApply
```

## Best Practices

- Follow DRY (Don't Repeat Yourself) principles
- Write clear, self-documenting code
- Add comments to explain *why*, not *what*
- Keep methods focused and reasonably sized
- Handle errors appropriately at the right abstraction level
