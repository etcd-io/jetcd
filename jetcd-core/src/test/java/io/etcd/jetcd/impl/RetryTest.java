package io.etcd.jetcd.impl;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.test.EtcdClusterExtension;
import io.grpc.StatusRuntimeException;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class RetryTest {

    private static final int CONNECTION_ATTEMPTS_BEFORE_RECOVERY = 5;

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

    @Test
    public void testReconnect() throws Exception {
        ClientBuilder builder = Client.builder()
            .endpoints("http://127.0.0.1:9999")
            .connectTimeout(Duration.ofMillis(250))
            .waitForReady(false)
            .retryMaxAttempts(5)
            .retryDelay(250);

        AtomicReference<Throwable> error = new AtomicReference<>();

        try (Client client = builder.build()) {
            CompletableFuture<?> unused = client.getKVClient().put(bytesOf("sample_key"), bytesOf("sample_value")).whenComplete(
                (r, t) -> {
                    if (t != null) {
                        error.set(t);
                    }
                });

            await().untilAsserted(() -> {
                assertThat(error.get()).isNotNull();
                assertThat(error.get()).hasCauseInstanceOf(StatusRuntimeException.class);
                assertThat(error.get().getCause()).hasMessage("UNAVAILABLE: io exception");
            });
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void testRetryResetsConnectBackoff() throws Exception {
        URI endpoint = cluster.clientEndpoints().get(0);

        try (TcpProxy proxy = new TcpProxy(endpoint.getHost(), endpoint.getPort())) {
            ClientBuilder builder = Client.builder()
                .endpoints(proxy.endpoint())
                .connectTimeout(Duration.ofSeconds(5))
                .waitForReady(false)
                .retryMaxAttempts(2)
                .retryDelay(500);

            try (Client client = builder.build()) {
                KV kv = client.getKVClient();

                await()
                    .atMost(Duration.ofSeconds(30))
                    .until(() -> {
                        kv.get(bytesOf("sample_key")).handle((r, t) -> null).get(10, TimeUnit.SECONDS);
                        return proxy.acceptedConnections() >= CONNECTION_ATTEMPTS_BEFORE_RECOVERY;
                    });

                int refusedConnections = proxy.acceptedConnections();
                await()
                    .atMost(Duration.ofSeconds(30))
                    .pollDelay(Duration.ZERO)
                    .pollInterval(Duration.ofMillis(10))
                    .until(() -> proxy.acceptedConnections() > refusedConnections);

                proxy.startForwarding();

                assertThat(kv.get(bytesOf("sample_key"))).succeedsWithin(Duration.ofSeconds(10));
            }
        }
    }
}
