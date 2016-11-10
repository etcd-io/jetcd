package com.coreos.jetcd;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.coreos.jetcd.api.AuthGrpc;
import com.coreos.jetcd.api.AuthenticateRequest;
import com.coreos.jetcd.api.AuthenticateResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;

import static com.coreos.jetcd.EtcdClientUtil.defaultChannelBuilder;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Etcd Client
 */
public class EtcdClient {
    private final List<String>                    endpoints;
    private final ManagedChannel                  channel;
    private final NameResolver.Factory            nameResolverFactory;
    private final Supplier<EtcdKV>                kvClient;
    private final Supplier<EtcdAuth>              authClient;
    private final Supplier<EtcdMaintenance>       maintenanceClient;
    private final Supplier<EtcdCluster>           clusterClient;

    public EtcdClient(EtcdClientBuilder builder) throws ConnectException, AuthFailedException {
        this(Optional.empty(), builder);
    }

    public EtcdClient(ManagedChannelBuilder<?> channelBuilder, EtcdClientBuilder clientBuilder) throws ConnectException, AuthFailedException {
        this(Optional.ofNullable(channelBuilder), clientBuilder);
    }

    private EtcdClient(Optional<ManagedChannelBuilder<?>> channelBuilder, EtcdClientBuilder clientBuilder) throws ConnectException, AuthFailedException {
        if (clientBuilder.getNameResolverFactory() != null) {
            this.endpoints = null;
            this.nameResolverFactory = clientBuilder.getNameResolverFactory();
        } else {
            //If no nameResolverFactory was set, use SimpleEtcdNameResolver
            this.endpoints = new ArrayList<>(clientBuilder.endpoints());
            this.nameResolverFactory = EtcdClientUtil.simpleNameResolveFactory(this.endpoints);
        }

        this.channel = channelBuilder.orElseGet(() -> defaultChannelBuilder(nameResolverFactory)).build();

        Optional<String> token = getToken(channel, clientBuilder);

        this.kvClient = Suppliers.memoize(() -> new EtcdKVImpl(channel, token));
        this.authClient =  Suppliers.memoize(() -> new EtcdAuthImpl(channel, token));
        this.maintenanceClient = Suppliers.memoize(() -> new EtcdMaintenanceImpl(channel, token));
        this.clusterClient =  Suppliers.memoize(() -> new EtcdClusterImpl(channel, token));
    }

    // ************************
    //
    // ************************

    public EtcdAuth getAuthClient() {
        return authClient.get();
    }

    public EtcdKV getKVClient() {
        return kvClient.get();
    }

    public EtcdCluster getClusterClient() {
        return clusterClient.get();
    }

    public EtcdMaintenance getMaintenanceClient() {
        return this.maintenanceClient.get();
    }

    public void close() {
        channel.shutdownNow();
    }

    // ************************
    //
    // ************************

    /**
     * get token from etcd with name and password
     *
     * @param channel  channel to etcd
     * @param name     auth name
     * @param password auth password
     * @return authResp
     */
    private static ListenableFuture<AuthenticateResponse> authenticate(ManagedChannel channel, ByteString name, ByteString password) {
        return AuthGrpc.newFutureStub(channel).authenticate(
            AuthenticateRequest.newBuilder()
                .setNameBytes(name)
                .setPasswordBytes(password)
                .build()
        );
    }

    /**
     * get token with EtcdClientBuilder
     *
     * @param builder
     * @return the auth token
     * @throws ConnectException    This may be caused as network reason, wrong address
     * @throws AuthFailedException This may be caused as wrong username or password
     */
    private static Optional<String> getToken(ManagedChannel channel, EtcdClientBuilder builder) throws ConnectException, AuthFailedException {
        if (builder.getName() != null || builder.getPassword() != null) {
            checkNotNull(builder.getName(), "username can not be null.");
            checkNotNull(builder.getPassword(), "password can not be null.");
            checkArgument(builder.getName().toStringUtf8().trim().length() != 0, "username can not be null.");
            checkArgument(builder.getPassword().toStringUtf8().trim().length() != 0, "password can not be null.");

            try {
                return Optional.of(authenticate(channel, builder.getName(), builder.getPassword()).get().getToken());
            } catch (InterruptedException ite) {
                throw new ConnectException("connect to etcd failed", ite);
            } catch (ExecutionException exee) {
                throw new AuthFailedException("auth failed as wrong username or password", exee);
            }
        }
        return Optional.empty();
    }
}
