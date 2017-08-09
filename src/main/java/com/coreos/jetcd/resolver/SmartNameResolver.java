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

  private final Object lock;
  private final String authority;
  private final List<URI> uris;
  private final List<URIResolver> resolvers;

  private volatile boolean shutdown;
  private volatile boolean resolving;

  @GuardedBy("lock")
  private ExecutorService executor;
  @GuardedBy("lock")
  private Listener listener;

  public SmartNameResolver(List<URI> uris) {
    this.lock = new Object();
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
    synchronized (lock) {
      Preconditions.checkState(this.listener == null, "already started");
      this.executor = SharedResourceHolder.get(GrpcUtil.SHARED_CHANNEL_EXECUTOR);
      this.listener = Preconditions.checkNotNull(listener, "listener");
      resolve();
    }
  }

  @Override
  public final synchronized void refresh() {
    resolve();
  }

  @Override
  public void shutdown() {
    if (shutdown) {
      return;
    }
    shutdown = true;

    synchronized (lock) {
      if (executor != null) {
        executor = SharedResourceHolder.release(GrpcUtil.SHARED_CHANNEL_EXECUTOR, executor);
      }
    }
  }

  private void resolve() {
    if (resolving || shutdown) {
      return;
    }
    synchronized (lock) {
      executor.execute(this::doResolve);
    }
  }

  private void doResolve() {
    Listener savedListener;
    synchronized (lock) {
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

      savedListener.onAddresses(groups, Attributes.EMPTY);
    } catch (Exception e) {
      LOGGER.warn("Error wile getting list of servers", e);
      savedListener.onError(Status.NOT_FOUND);
    } finally {
      resolving = false;
    }
  }
}
