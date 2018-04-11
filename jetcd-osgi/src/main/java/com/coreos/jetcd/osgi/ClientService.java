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

package com.coreos.jetcd.osgi;

import java.util.HashSet;
import java.util.Set;

import com.coreos.jetcd.Auth;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.Cluster;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Lock;
import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.resolver.URIResolver;
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
        configurationPid = "com.coreos.jetcd"
)
@Designate(ocd = ClientService.Configuration.class)
public class ClientService implements Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientService.class);
    private final Set<URIResolver> resolvers;

    private Client delegate;

    public ClientService() {
        this.resolvers = new HashSet<>();
    }

    public Auth getAuthClient() {
        return delegate.getAuthClient();
    }

    public KV getKVClient() {
        return delegate.getKVClient();
    }

    public Cluster getClusterClient() {
        return delegate.getClusterClient();
    }

    public Maintenance getMaintenanceClient() {
        return delegate.getMaintenanceClient();
    }

    public Lease getLeaseClient() {
        return delegate.getLeaseClient();
    }

    public Watch getWatchClient() {
        return delegate.getWatchClient();
    }

    public Lock getLockClient() {
        return delegate.getLockClient();
    }

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
            builder.user(ByteSequence.fromString(config.user()));
        }
        if (config.password() != null) {
            builder.password(ByteSequence.fromString(config.password()));
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
    }
}
