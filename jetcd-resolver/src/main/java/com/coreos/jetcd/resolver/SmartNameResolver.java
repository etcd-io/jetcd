/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coreos.jetcd.resolver;

import com.coreos.jetcd.common.exception.ErrorCode;
import com.coreos.jetcd.common.exception.EtcdExceptionFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.concurrent.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartNameResolver extends NameResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmartNameResolver.class);

  private final Object lock;
  private final String authority;
  private final Collection<URI> uris;
  private final List<URIResolver> resolvers;

  private volatile boolean shutdown;
  private volatile boolean resolving;

  @GuardedBy("lock")
  private ExecutorService executor;
  @GuardedBy("lock")
  private Listener listener;

  public SmartNameResolver(String authority, Collection<URI> uris, URIResolverLoader loader) {
    this.lock = new Object();
    this.authority = authority;
    this.uris = uris;

    this.resolvers = new ArrayList<>();
    this.resolvers.add(new DirectUriResolver());
    this.resolvers.addAll(loader.load());
    this.resolvers.sort(Comparator.comparingInt(r -> r.priority()));
  }

  @VisibleForTesting
  public List<URIResolver> getResolvers() {
    return Collections.unmodifiableList(resolvers);
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
            .map(EquivalentAddressGroup::new)
            .forEach(groups::add);
      }

      if (groups.isEmpty()) {
        throw EtcdExceptionFactory.newEtcdException(
            ErrorCode.INVALID_ARGUMENT,
            ("Unable to resolve endpoints " + uris)
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
