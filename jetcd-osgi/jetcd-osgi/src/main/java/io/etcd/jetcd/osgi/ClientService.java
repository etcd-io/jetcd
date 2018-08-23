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

package io.etcd.jetcd.osgi;

import io.etcd.jetcd.Auth;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.Cluster;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.Maintenance;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.resolver.URIResolver;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    service = Client.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    configurationPid = "io.etcd.jetcd"
)
@Designate(ocd = ClientService.Configuration.class)
public class ClientService implements Client {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientService.class);
  private final Set<URIResolver> resolvers;

  private Client delegate;

  public ClientService() {
    this.resolvers = new HashSet<>();
  }

  @Override
  public Auth getAuthClient() {
    return delegate.getAuthClient();
  }

  @Override
  public KV getKVClient() {
    return delegate.getKVClient();
  }

  @Override
  public Cluster getClusterClient() {
    return delegate.getClusterClient();
  }

  @Override
  public Maintenance getMaintenanceClient() {
    return delegate.getMaintenanceClient();
  }

  @Override
  public Lease getLeaseClient() {
    return delegate.getLeaseClient();
  }

  @Override
  public Watch getWatchClient() {
    return delegate.getWatchClient();
  }

  @Override
  public Lock getLockClient() {
    return delegate.getLockClient();
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("");
  }

  // **********************
  // Lifecycle
  // **********************

  @Activate
  protected void activate(Configuration config) {
    ClientBuilder builder = Client.builder();

    builder.endpoints(config.endpoints());
    builder.uriResolverLoader(() -> resolvers);

    if (config.user() != null) {
      builder.user(ByteSequence.from(config.user(), Charset.forName(config.charset())));
    }
    if (config.password() != null) {
      builder.password(ByteSequence.from(config.password(), Charset.forName(config.charset())));
    }

    this.delegate = builder.build();
  }

  @Deactivate
  protected void deactivate() {
    if (this.delegate != null) {
      this.delegate.close();
    }
  }

  @Reference(
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC
  )
  protected void bindResolver(URIResolver resolver) {
    LOGGER.debug("Adding resolver: {}", resolver);
    this.resolvers.add(resolver);
  }

  protected void unbindResolver(URIResolver resolver) {
    LOGGER.debug("Remove resolver: {}", resolver);
    this.resolvers.remove(resolver);
  }

  // **********************
  // Configuration
  // **********************

  @ObjectClassDefinition
  public @interface Configuration {
    @AttributeDefinition
    String[] endpoints();

    @AttributeDefinition(required = false)
    String user();

    @AttributeDefinition(required = false, type = AttributeType.PASSWORD)
    String password();

    @AttributeDefinition(required = false, type = AttributeType.STRING, defaultValue = "UTF-8", description = "Character Set used for user and password")
    String charset();
  }
}
