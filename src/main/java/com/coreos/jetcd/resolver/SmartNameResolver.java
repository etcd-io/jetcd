package com.coreos.jetcd.resolver;

import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import javax.annotation.concurrent.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartNameResolver extends NameResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(SmartNameResolver.class);

  private final String authority;
  private final List<URI> uris;
  private final List<URIResolver> resolvers;

  @GuardedBy("this")
  private boolean shutdown;
  @GuardedBy("this")
  private boolean resolving;
  @GuardedBy("this")
  private Listener listener;
  @GuardedBy("this")
  private ExecutorService executor;

  public SmartNameResolver(List<URI> uris) {
    this.authority = "etcd";
    this.uris = uris;
    this.resolvers = new ArrayList<>();

    Iterator<URIResolver> it = ServiceLoader.load(URIResolver.class).iterator();
    while (it.hasNext()) {
      this.resolvers.add(it.next());
    }

    this.resolvers.sort(Comparator.comparingInt(r -> r.priority()));
  }

  @Override
  public String getServiceAuthority() {
    return authority;
  }

  @Override
  public void start(Listener listener) {
    Preconditions.checkState(this.listener == null, "already started");
    this.executor = SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);
    this.listener = Preconditions.checkNotNull(listener, "listener");
    resolve();
  }

  @Override
  public final synchronized void refresh() {
    Preconditions.checkState(listener != null, "not started");
    resolve();
  }

  @Override
  public void shutdown() {
    if (shutdown) {
      return;
    }
    shutdown = true;
    if (executor != null) {
      executor = SharedResourceHolder.release(GrpcUtil.SHARED_CHANNEL_EXECUTOR, executor);
    }
  }

  @GuardedBy("this")
  private void resolve() {
    if (resolving || shutdown) {
      return;
    }
    executor.execute(this::doResolve);
  }

  private void doResolve() {
    Listener savedListener;
    synchronized (SmartNameResolver.this) {
      if (shutdown) {
        return;
      }
      resolving = true;
      savedListener = listener;
    }

    try {
      List<EquivalentAddressGroup> groups = new ArrayList<>();

      for (URI uri : uris) {
        resolvers.stream()
            .filter(r -> r.supports(uri))
            .limit(1)
            .flatMap(r -> r.resolve(uri).stream())
            .forEach(groups::add);
      }

      if (groups.isEmpty()) {
        throw EtcdExceptionFactory.newEtcdException(
            new IllegalStateException("Unable to resolve endpoints " + uris)
        );
      }

      listener.onAddresses(groups, Attributes.EMPTY);
    } catch (Exception e) {
      LOGGER.warn("Error wile getting list of servers", e);
      savedListener.onError(Status.NOT_FOUND);
    } finally {
      synchronized (SmartNameResolver.this) {
        resolving = false;
      }
    }
  }
}
