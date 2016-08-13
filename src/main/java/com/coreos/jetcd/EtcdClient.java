package com.coreos.jetcd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ExecutionException;

import com.coreos.jetcd.api.AuthGrpc;
import com.coreos.jetcd.api.AuthenticateRequest;
import com.coreos.jetcd.api.AuthenticateResponse;
import com.coreos.jetcd.api.KVGrpc;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;

/**
 * Etcd Client
 */
public class EtcdClient {

    private static final String            TOKEN = "token";

    private final ManagedChannelBuilder<?> channelBuilder;
    private final String[]                 endpoints;
    private final ManagedChannel           channel;

    private final EtcdKV                   kvClient;
    private final EtcdAuth                 authClient;

    private KVGrpc.KVFutureStub            kvStub;
    private AuthGrpc.AuthFutureStub        authStub;

    public EtcdClient(ManagedChannelBuilder<?> channelBuilder, EtcdClientBuilder builder) throws ConnectException, AuthFailedException {
        this.endpoints = new String[builder.endpoints().size()];
        builder.endpoints().toArray(this.endpoints);
        this.channelBuilder = channelBuilder != null ? channelBuilder : ManagedChannelBuilder.forAddress("localhost", 2379).usePlaintext(
            true);

        this.channel = this.channelBuilder.build();

        this.kvStub = KVGrpc.newFutureStub(this.channel);
        this.authStub = AuthGrpc.newFutureStub(this.channel);

        String token = getToken(builder);

        if (token != null) {
            this.authStub = setTokenForStub(authStub, token);
            this.kvStub = setTokenForStub(kvStub, token);
        }

        this.kvClient = newKVClient(kvStub);
        this.authClient = newAuthClient(authStub);
    }

    /**
     * create a new KV client.
     *
     * @return new KV client
     */
    public EtcdKV newKVClient(KVGrpc.KVFutureStub stub) {
        return new EtcdKVImpl(stub);
    }

    private EtcdAuth newAuthClient(AuthGrpc.AuthFutureStub stub) {
        return new EtcdAuthImpl(stub);
    }

    protected EtcdAuth getAuthClient() {
        return authClient;
    }

    protected EtcdKV getKVClient() {
        return kvClient;
    }

    /**
     * add token to channel's head
     *
     * @param stub  the stub to attach head
     * @param token the token for auth
     * @param <T>   the type of stub
     * @return the attached stub
     */
    private <T extends AbstractStub<?>> T setTokenForStub(T stub, String token) {
        Metadata metadata = new Metadata();
        Metadata.Key<String> TOKEN_KEY = Metadata.Key.of(TOKEN, Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(TOKEN_KEY, token);
        CallCredentials callCredentials = (methodDescriptor, attributes, executor, metadataApplier) -> metadataApplier.apply(metadata);
        return ((T) stub.withCallCredentials(callCredentials));
    }

    /**
     * get token from etcd with name and password
     *
     * @param channel  channel to etcd
     * @param name     auth name
     * @param password auth password
     * @return authResp
     */
    private ListenableFuture<AuthenticateResponse> authenticate(ManagedChannel channel, ByteString name, ByteString password) {

        ListenableFuture<AuthenticateResponse> authResp = AuthGrpc.newFutureStub(channel).authenticate(
            AuthenticateRequest.newBuilder().setNameBytes(name).setPasswordBytes(password).build());
        return authResp;
    }

    /**
     * get token with EtcdClientBuilder
     *
     * @param builder
     * @return the auth token
     * @throws ConnectException    This may be caused as network reason, wrong address
     * @throws AuthFailedException This may be caused as wrong username or password
     */
    private String getToken(EtcdClientBuilder builder) throws ConnectException, AuthFailedException {

        if (builder.getName() != null || builder.getPassword() != null) {

            checkNotNull(builder.getName(), "username can not be null.");
            checkNotNull(builder.getPassword(), "password can not be null.");
            checkArgument(builder.getName().toStringUtf8().trim().length() != 0, "username can not be null.");
            checkArgument(builder.getPassword().toStringUtf8().trim().length() != 0, "password can not be null.");

            try {
                return authenticate(this.channel, builder.getName(), builder.getPassword()).get().getToken();
            } catch (InterruptedException ite) {
                throw new ConnectException("connect to etcd failed", ite);
            } catch (ExecutionException exee) {
                throw new AuthFailedException("auth failed as wrong username or password", exee);
            }
        }
        return null;
    }

    public void close() {
        channel.shutdownNow();
    }

}
