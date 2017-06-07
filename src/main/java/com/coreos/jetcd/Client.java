package com.coreos.jetcd;

import static com.coreos.jetcd.ClientUtil.defaultChannelBuilder;
import static com.coreos.jetcd.Util.byteStringFromByteSequence;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.coreos.jetcd.api.AuthGrpc;
import com.coreos.jetcd.api.AuthenticateRequest;
import com.coreos.jetcd.api.AuthenticateResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.coreos.jetcd.internal.Pair;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Etcd Client.
 */
public class Client {

  private final NameResolver.Factory nameResolverFactory;
  private final Supplier<KV> kvClient;
  private final Supplier<Auth> authClient;
  private final Supplier<Maintenance> maintenanceClient;
  private final Supplier<Cluster> clusterClient;
  private final Supplier<Lease> leaseClient;
  private final Supplier<Watch> watchClient;
  private final ByteString user;
  private final ByteString pass;
  private final ManagedChannel channel;
  private final Optional<String> token;

  // shared executorService
  ExecutorService executorService = Executors.newCachedThreadPool();

  ExecutorService getExecutorService() {
    return this.executorService;
  }

  ManagedChannel getChannel() {
    return this.channel;
  }

  Optional<String> getToken() {
    return this.token;
  }

  public Client(ClientBuilder builder) throws ConnectException, AuthFailedException {
    this(Optional.empty(), builder);
  }

  public Client(ManagedChannelBuilder<?> channelBuilder, ClientBuilder clientBuilder)
      throws ConnectException, AuthFailedException {
    this(Optional.ofNullable(channelBuilder), clientBuilder);
  }

  private Client(Optional<ManagedChannelBuilder<?>> channelBuilderOptional,
      ClientBuilder clientBuilder) throws ConnectException, AuthFailedException {
    if (clientBuilder.getNameResolverFactory() != null) {
      this.nameResolverFactory = clientBuilder.getNameResolverFactory();
    } else {
      //If no nameResolverFactory was set, use SimpleEtcdNameResolver
      checkNotNull(clientBuilder.endpoints(), "endpoints can't be null");
      this.nameResolverFactory = ClientUtil.simpleNameResolveFactory(clientBuilder.endpoints());
    }

    if (clientBuilder.getName() != null && clientBuilder.getPassword() != null) {
      this.user = byteStringFromByteSequence(clientBuilder.getName());
      this.pass = byteStringFromByteSequence(clientBuilder.getPassword());
    } else {
      this.user = null;
      this.pass = null;
    }

    Pair<ManagedChannel, Optional<String>> channelToken = this
        .toChannelAndToken(
            channelBuilderOptional.orElse(defaultChannelBuilder(nameResolverFactory)));
    this.channel = channelToken.getKey();
    this.token = channelToken.getValue();
    // TODO: *Impl constructor should take client as agrument to its constructor.
    this.kvClient = Suppliers.memoize(() -> new KVImpl(channel, token));
    this.authClient = Suppliers.memoize(() -> new AuthImpl(channel, token));
    this.maintenanceClient = Suppliers.memoize(() -> new MaintenanceImpl(this));
    this.clusterClient = Suppliers.memoize(() -> new ClusterImpl(channel, token));
    this.leaseClient = Suppliers.memoize(() -> new LeaseImpl(channel, token));
    this.watchClient = Suppliers.memoize(() -> new WatchImpl(channel, token));
  }

  Pair<ManagedChannel, Optional<String>> toChannelAndToken(ManagedChannelBuilder<?> channelBuilder)
      throws AuthFailedException, ConnectException {
    checkNotNull(channelBuilder, "channelBuilder can't be null");
    ManagedChannel managedChannel = channelBuilder.build();
    Optional<String> token = this.generateToken(managedChannel, this.user, this.pass);
    return new Pair<>(managedChannel, token);
  }

  public Auth getAuthClient() {
    return authClient.get();
  }

  public KV getKVClient() {
    return kvClient.get();
  }

  public Cluster getClusterClient() {
    return clusterClient.get();
  }

  public Maintenance getMaintenanceClient() {
    return this.maintenanceClient.get();
  }

  public Lease getLeaseClient() {
    return this.leaseClient.get();
  }

  public Watch getWatchClient() {
    return this.watchClient.get();
  }

  public void close() {
    this.executorService.shutdownNow();
    this.channel.shutdownNow();
  }

  /**
   * get token from etcd with name and password.
   *
   * @param channel channel to etcd
   * @param name auth name
   * @param password auth password
   * @return authResp
   */
  private static ListenableFuture<AuthenticateResponse> authenticate(ManagedChannel channel,
      ByteString name, ByteString password) {
    return AuthGrpc.newFutureStub(channel).authenticate(
        AuthenticateRequest.newBuilder()
            .setNameBytes(name)
            .setPasswordBytes(password)
            .build()
    );
  }

  /**
   * get token with ClientBuilder.
   *
   * @return the auth token
   * @throws ConnectException This may be caused as network reason, wrong address
   * @throws AuthFailedException This may be caused as wrong username or password
   */
  private static Optional<String> generateToken(ManagedChannel channel, ByteString user,
      ByteString pass)
      throws ConnectException, AuthFailedException {
    if (user != null && pass != null) {
      checkArgument(!user.isEmpty(),
          "username can not be empty.");
      checkArgument(!pass.isEmpty(),
          "password can not be empty.");

      try {
        return Optional
            .of(authenticate(channel, user, pass).get().getToken());
      } catch (InterruptedException ite) {
        throw new ConnectException("connect to etcd failed", ite);
      } catch (ExecutionException exee) {
        throw new AuthFailedException("auth failed as wrong username or password", exee);
      }
    }
    return Optional.empty();
  }
}
