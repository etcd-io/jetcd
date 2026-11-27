package io.etcd.jetcd.impl.auth;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.SecureByteSequence;
import io.etcd.jetcd.auth.Permission;
import io.etcd.jetcd.common.vertx.Ssl;
import io.etcd.jetcd.impl.TestUtil;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;

@Timeout(value = 30)
public class AuthTokenRefreshTest {

    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .withSsl(true)
        .withAdditionalArgs(
            List.of(
                "--auth-token",
                "jwt,pub-key=/etc/ssl/etcd/server.pem,priv-key=/etc/ssl/etcd/server-key.pem,sign-method=RS256,ttl=300s"))
        .build();

    private static final ByteSequence key = TestUtil.bytesOf("key");
    private static final ByteSequence keyEnd = TestUtil.bytesOf("key1");
    private static final ByteSequence user = TestUtil.bytesOf("root");
    private static final ByteSequence password = TestUtil.randomByteSequence();

    @Test
    public void testTokenIsRefreshedWhenRevisionsIsOld() throws Exception {
        setUpEnvironment();

        try (Client authClient = createAuthClient()) {
            KV kvClient = authClient.getKVClient();
            kvClient.put(key, TestUtil.randomByteSequence()).get(5, TimeUnit.SECONDS);

            // Adding a role advances the auth revision and invalidates the cached JWT.
            authClient.getAuthClient().roleAdd(TestUtil.bytesOf("newRole")).get(5, TimeUnit.SECONDS);

            // The default put can retry after etcd confirms it rejected the stale token.
            ByteSequence replacement = TestUtil.randomByteSequence();
            kvClient.put(key, replacement).get(5, TimeUnit.SECONDS);

            GetResponse getResponse = kvClient.get(key).get(5, TimeUnit.SECONDS);
            Assertions.assertEquals(1, getResponse.getKvs().size());
            Assertions.assertEquals(key, getResponse.getKvs().get(0).getKey());
            Assertions.assertEquals(replacement, getResponse.getKvs().get(0).getValue());
        }
    }

    private void setUpEnvironment() throws Exception {
        final File caFile = new File(Objects.requireNonNull(getClass().getResource("/ssl/cert/ca.pem")).toURI());

        Client client = TestUtil.client(cluster)
            .httpClientOptions(Ssl.withTrustManager(caFile)
                .andThen(options -> options.setVerifyHost(false)))
            .build();

        // enable authentication to enforce usage of access token
        ByteSequence role = TestUtil.bytesOf("root");
        client.getAuthClient().roleAdd(role).get();
        client.getAuthClient().userAdd(user, password).get();
        // grant access only to given key
        client.getAuthClient().roleGrantPermission(role, key, keyEnd, Permission.Type.READWRITE).get();
        client.getAuthClient().userGrantRole(user, role).get();
        client.getAuthClient().authEnable().get();

        client.close();
    }

    private Client createAuthClient() throws Exception {
        final File caFile = new File(Objects.requireNonNull(getClass().getResource("/ssl/cert/ca.pem")).toURI());

        return TestUtil.client(cluster)
            .user(SecureByteSequence.from(user.getBytes()))
            .password(SecureByteSequence.from(password.getBytes()))
            .httpClientOptions(Ssl.withTrustManager(caFile)
                .andThen(options -> options.setVerifyHost(false)))
            .build();
    }

}
