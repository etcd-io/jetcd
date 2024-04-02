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
import io.etcd.jetcd.auth.Permission;
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

        Client authClient = createAuthClient();
        KV kvClient = authClient.getKVClient();

        ByteSequence value = TestUtil.randomByteSequence();
        kvClient.put(key, value).get(1, TimeUnit.SECONDS);

        //read the created key to generate a new jwt token
        GetResponse getResponse = kvClient.get(key).get(1, TimeUnit.SECONDS);
        Assertions.assertEquals(1, getResponse.getKvs().size());
        Assertions.assertEquals(key, getResponse.getKvs().get(0).getKey());
        Assertions.assertEquals(value, getResponse.getKvs().get(0).getValue());

        //update auth database revision by adding a new role thus invalidating existing token of the kv client
        authClient.getAuthClient().roleAdd(TestUtil.bytesOf("newRole")).get(1, TimeUnit.SECONDS);

        //kv client should automatically refresh the old token and read the key
        getResponse = kvClient.get(key).get(1, TimeUnit.SECONDS);

        Assertions.assertEquals(1, getResponse.getKvs().size());
        Assertions.assertEquals(key, getResponse.getKvs().get(0).getKey());
        Assertions.assertEquals(value, getResponse.getKvs().get(0).getValue());
    }

    private void setUpEnvironment() throws Exception {
        final File caFile = new File(Objects.requireNonNull(getClass().getResource("/ssl/cert/ca.pem")).toURI());

        Client client = TestUtil.client(cluster)
            .authority("etcd0")
            .sslContext(b -> b.trustManager(caFile))
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
            .user(user)
            .password(password)
            .authority("etcd0")
            .sslContext(b -> b.trustManager(caFile))
            .build();
    }

}
