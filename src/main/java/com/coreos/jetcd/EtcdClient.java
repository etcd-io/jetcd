package com.coreos.jetcd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import com.coreos.jetcd.api.*;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.resolver.AbstractEtcdNameResolverFactory;
import com.coreos.jetcd.resolver.SimpleEtcdNameResolverFactory;
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

    private static final String                   TOKEN = "token";

    private final ManagedChannelBuilder<?>        channelBuilder;
    private final String[]                        endpoints;
    private final ManagedChannel                  channel;
    private final AbstractEtcdNameResolverFactory nameResolverFactory;

    private final EtcdKV                          kvClient;
    private final EtcdAuth                        authClient;
    private final EtcdMaintenance                 maintenanceClient;
    private final EtcdCluster                     clusterClient;

    private KVGrpc.KVFutureStub                   kvStub;
    private AuthGrpc.AuthFutureStub               authStub;

    public EtcdClient(ManagedChannelBuilder<?> channelBuilder, EtcdClientBuilder builder) throws ConnectException, AuthFailedException {
        if (builder.getNameResolverFactory() != null) {
            endpoints = null;
            this.nameResolverFactory = builder.getNameResolverFactory();
        } else {
            //If no nameResolverFactory was set, use SimpleEtcdNameResolver
            this.endpoints = new String[builder.endpoints().size()];
            builder.endpoints().toArray(this.endpoints);
            this.nameResolverFactory = getSimpleNameResolveFactory(this.endpoints);
        }

        this.channelBuilder = channelBuilder != null ? channelBuilder : ManagedChannelBuilder.forTarget("etcd")
            .nameResolverFactory(nameResolverFactory).usePlaintext(true);
        this.channel = this.channelBuilder.build();

        this.kvStub = KVGrpc.newFutureStub(this.channel);
        this.authStub = AuthGrpc.newFutureStub(this.channel);
        MaintenanceGrpc.MaintenanceFutureStub mainFStub = MaintenanceGrpc.newFutureStub(this.channel);
        MaintenanceGrpc.MaintenanceStub mainStub = MaintenanceGrpc.newStub(this.channel);
        ClusterGrpc.ClusterFutureStub clusterStub = ClusterGrpc.newFutureStub(this.channel);

        String token = getToken(builder);

        if (token != null) {
            this.authStub = setTokenForStub(authStub, token);
            this.kvStub = setTokenForStub(kvStub, token);
            mainFStub = setTokenForStub(mainFStub, token);
            mainStub = setTokenForStub(mainStub, token);
            clusterStub = setTokenForStub(clusterStub, token);
        }

        this.kvClient = newKVClient(kvStub);
        this.authClient = newAuthClient(authStub);
        this.maintenanceClient = newMaintenanceClient(mainFStub, mainStub);
        this.clusterClient = newClusterClient(clusterStub);
    }

    /**
     * create a new KV client.
     *
     * @return new KV client
     */
    private EtcdKV newKVClient(KVGrpc.KVFutureStub stub) {
        return new EtcdKVImpl(stub);
    }

    private EtcdAuth newAuthClient(AuthGrpc.AuthFutureStub stub) {
        return new EtcdAuthImpl(stub);
    }

    private EtcdCluster newClusterClient(ClusterGrpc.ClusterFutureStub stub) {
        return new EtcdClusterImpl(stub);
    }

    private EtcdMaintenance newMaintenanceClient(MaintenanceGrpc.MaintenanceFutureStub futureStub, MaintenanceGrpc.MaintenanceStub stub) {
        return new EtcdMaintenanceImpl(futureStub, stub);
    }

    public EtcdAuth getAuthClient() {
        return authClient;
    }

    public EtcdKV getKVClient() {
        return kvClient;
    }

    public EtcdCluster getClusterClient() {
        return clusterClient;
    }

    public EtcdMaintenance getMaintenanceClient() {
        return this.maintenanceClient;
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

    private AbstractEtcdNameResolverFactory getSimpleNameResolveFactory(String[] endpoints) {
        URI[] uris = new URI[endpoints.length];
        for (int i = 0; i < endpoints.length; ++i) {
            try {
                String endpoint = endpoints[i];
                if (!endpoint.startsWith("http://")) {
                    endpoint = "http://" + endpoint;
                }
                uris[i] = new URI(endpoint);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return new SimpleEtcdNameResolverFactory(uris);
    }

}
