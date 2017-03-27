package com.coreos.jetcd.resolver;

import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.ResolvedServerInfo;
import io.grpc.ResolvedServerInfoGroup;
import io.grpc.internal.SharedResourceHolder;
import io.grpc.internal.SharedResourceHolder.Resource;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.concurrent.GuardedBy;

/**
 * The abstract etcd name resolver, all other name resolvers should
 * extend this one instead of {@link NameResolver}.
 */
public abstract class AbstractEtcdNameResolver extends NameResolver {

  private final String authority;
  private final Resource<ExecutorService> executorResource;
  private final Runnable resolutionRunnable;

  @GuardedBy("this")
  private boolean shutdown;
  @GuardedBy("this")
  private boolean resolving;
  @GuardedBy("this")
  private Listener listener;
  @GuardedBy("this")
  private ExecutorService executor;

  public AbstractEtcdNameResolver(String name, Resource<ExecutorService> executorResource) {
    URI nameUri = URI.create("//" + name);

    this.executorResource = executorResource;
    this.authority = Preconditions
        .checkNotNull(nameUri.getAuthority(), "nameUri (%s) doesn't have an authority", nameUri);
    this.resolutionRunnable = new ResolverTask();
  }

  @Override
  public String getServiceAuthority() {
    return authority;
  }

  @Override
  public final synchronized void start(Listener listener) {
    Preconditions.checkState(this.listener == null, "already started");
    this.executor = SharedResourceHolder.get(executorResource);
    this.listener = Preconditions.checkNotNull(listener, "listener");
    resolve();
  }

  @Override
  public final synchronized void refresh() {
    Preconditions.checkState(listener != null, "not started");
    resolve();
  }

  @Override
  public final synchronized void shutdown() {
    if (shutdown) {
      return;
    }
    shutdown = true;
    if (executor != null) {
      executor = SharedResourceHolder.release(executorResource, executor);
    }
  }

  @GuardedBy("this")
  private void resolve() {
    if (resolving || shutdown) {
      return;
    }
    executor.execute(resolutionRunnable);
  }

  protected abstract List<ResolvedServerInfo> getServers();

  /**
   * Helper task to resolve servers.
   */
  private final class ResolverTask implements Runnable {

    @Override
    public void run() {
      Listener savedListener;
      synchronized (AbstractEtcdNameResolver.this) {
        if (shutdown) {
          return;
        }
        resolving = true;
        savedListener = listener;
      }

      try {
        List<ResolvedServerInfo> servers = getServers();
        savedListener.onUpdate(
            Collections.singletonList(ResolvedServerInfoGroup.builder().addAll(servers).build()),
            Attributes.EMPTY);
      } finally {
        synchronized (AbstractEtcdNameResolver.this) {
          resolving = false;
        }
      }
    }
  }
}