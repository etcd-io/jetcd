# Upgrade to the Vert.x gRPC client

Use this guide when upgrading an application from a grpc-java-based version of jetcd. Update the application to Java 21 or later and recompile it against the new client API. The Maven coordinate is still `io.etcd:jetcd-core`. [Transport migration issue](https://github.com/etcd-io/jetcd/issues/1372)

## Align the runtime dependencies

jetcd uses Vert.x 5, which brings Netty 4.2. If your application manages these versions through a framework, BOM, or explicit constraints, align its Vert.x and Netty modules with the versions resolved for jetcd. In particular, a Netty 4.1 constraint can cause linkage errors when Vert.x starts. Remove grpc-java dependencies that you added solely for jetcd and keep the ones required by other parts of your application. [Netty compatibility issue](https://github.com/etcd-io/jetcd/issues/1542) · [Vert.x 5 migration guide](https://vertx.io/docs/guides/vertx-5-migration-guide/)

Inspect your application's resolved runtime dependencies with Maven or Gradle.

```sh
mvn dependency:tree -Dscope=runtime '-Dincludes=io.vertx:*,io.netty:*'
./gradlew dependencyInsight --dependency io.netty --configuration runtimeClasspath
```

## Configure the client

| Previous API | Upgrade |
| --- | --- |
| `Client.builder().endpoints(...)` | Pass endpoint URLs or a collection of `URI` values to `Client.builder(...)`. |
| `Client.builder().target(...)` | Pass explicit endpoints or a `ServiceResolver` to `Client.builder(...)`. Use `ServiceResolvers.dnsSrv(...)` if your deployment publishes SRV records. |
| `loadBalancerPolicy("round_robin")` | Pass a Vert.x `LoadBalancer` to `loadBalancer(...)`. The default is `LoadBalancer.ROUND_ROBIN`. |
| `sslContext(...)` | Set Vert.x `HttpClientOptions` with `httpClientOptions(...)`. The `Ssl` helpers configure PEM certificates and ALPN. |
| `headers(Map<Metadata.Key<?>, Object>)` | Use `headers(Map<String, String>)` or `header(String, String)` for fixed text metadata. |
| `interceptor(...)`, `interceptors(...)`, `authInterceptors(...)`, `authHeaders(...)`, `authHeader(...)` | Instrument calls in your application. Fixed headers apply to client operations, while jetcd constructs automatic token requests separately. |
| `user(ByteSequence)`, `password(ByteSequence)` | Pass `SecureByteSequence` instances for credentials and close them after the client shuts down. |

Use endpoint hostnames that match the server certificate. This example uses mTLS, etcd username and password authentication, and a fixed header for KV and watch operations. `username` and `passwordChars` come from your application's credential source. [TLS configuration](design/ssl.md) · [DNS SRV configuration](design/dns-srv.md)

```java
import io.etcd.jetcd.Client;
import io.etcd.jetcd.SecureByteSequence;
import io.etcd.jetcd.common.vertx.Ssl;

try (SecureByteSequence user = SecureByteSequence.from(username);
     SecureByteSequence password = SecureByteSequence.from(passwordChars);
     Client client = Client.builder("https://etcd.example.com:2379")
         .httpClientOptions(Ssl.withTrustAndKeyManager(
             "/etc/etcd/pki/ca.crt", "/etc/etcd/pki/client.crt", "/etc/etcd/pki/client.key"))
         .header("x-client-name", "config-service")
         .user(user)
         .password(password)
         .build()) {
    // Use the client while the credentials are open.
}
```

Remove calls to `useVertx()`. If you pass a Vert.x instance with `vertx(vertx)`, the application owns that instance. Close the jetcd client before shutting down the shared Vert.x instance.

## Configure connection timing

- `connectTimeout(Duration)` controls TCP connection establishment for cluster and direct member requests. It overrides the TCP timeout supplied through `httpClientOptions(...)`. Configure TLS handshake timing separately through `HttpClientOptions`. [Vert.x client options](https://vertx.io/docs/5.2.3/apidocs/io/vertx/core/http/HttpClientOptions.html)
- Remove `waitForReady(...)`. For retryable operations, use `retryMaxAttempts` and `retryMaxDuration` to configure retries after a failure. [gRPC wait-for-ready](https://grpc.io/docs/guides/wait-for-ready/)
- KV writes retry after a confirmed invalid token or stale auth revision. Use `withAutoRetry()` on the write options if your application can safely repeat the write after a connection failure or `UNAVAILABLE`. A generic `UNKNOWN` or `INVALID_ARGUMENT` response does not trigger a retry.
- Remove `keepaliveTime(...)` and `keepaliveWithoutCalls(...)`. The Vert.x client's `setHttp2KeepAliveTimeout(...)` controls how long an idle pooled connection stays open. Periodic gRPC HTTP/2 pings have separate semantics. [gRPC keepalive](https://grpc.io/docs/guides/keepalive/) · [Vert.x HTTP/2 pool timeout](https://vertx.io/docs/5.2.3/apidocs/io/vertx/core/http/HttpClientOptions.html#setHttp2KeepAliveTimeout(int))
- Use `leaseKeepAliveTimeout(Duration)` in place of `keepaliveTimeout(Duration)` to limit the wait for the first lease renewal response.
- Lease grants retry only after a confirmed token rejection. The timeout on `grant(ttl, timeout, unit)` covers authentication, retries, and the grant response.

## Migrate streaming callbacks

Replace `StreamObserver<LeaseKeepAliveResponse>` with `Lease.Listener` or the callback overload of `keepAlive`. Keep the registration handle and close it when you no longer need automatic renewal.

```java
import io.etcd.jetcd.support.CloseableClient;

CloseableClient keepAlive = client.getLeaseClient().keepAlive(
    leaseId,
    response -> System.out.println(response.getTTL()),
    error -> System.err.println(error.getMessage()),
    () -> System.out.println("Lease keepalive completed"));
```

Jetcd delivers callbacks serially for each lease registration on Vert.x workers. A slow listener may skip intermediate heartbeats and reconnect errors while the client continues renewing the lease. Terminal notifications follow the callbacks still pending for the registration. Use `keepAliveOnce(leaseId)` when you need a single renewal response.

Other streaming APIs use the same listener pattern. Migrate `snapshot(StreamObserver<SnapshotResponse>)` to `snapshot(Maintenance.Listener)` or the callback overload. If you only need to write a backup, use `snapshot(OutputStream)` and close the caller-owned stream after the future completes. For a watch that must observe a subsequent write, use `watchAsync(...)` and chain the write after etcd confirms the watch. Keep the returned watcher so you can close it. [Watch behavior](design/watch.md)

If your application inspected `io.grpc.Status` for etcd operation failures, handle `EtcdException` and its `getErrorCode()` instead. A failed `CompletableFuture` may wrap the exception.

Election calls report `NoLeaderException` or `NotLeaderException` for the corresponding etcd response. Any other RPC failure carries its original status, server message, and cause in an `EtcdException`.
