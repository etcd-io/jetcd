package io.etcd.jetcd.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.grpc.StatusRuntimeException;

import static io.etcd.jetcd.impl.TestUtil.bytesOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class RetryTest {
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
}
