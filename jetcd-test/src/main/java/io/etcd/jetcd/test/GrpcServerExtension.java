/*
 * Copyright 2016-2020 The jetcd authors
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

package io.etcd.jetcd.test;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.google.common.base.Preconditions.checkState;

public class GrpcServerExtension implements BeforeEachCallback, AfterEachCallback {
    private ManagedChannel channel;
    private Server server;
    private String serverName;
    private MutableHandlerRegistry serviceRegistry;
    private boolean useDirectExecutor;

    /**
     * Returns {@code this} configured to use a direct executor for the {@link ManagedChannel} and
     * {@link Server}. This can only be called at the rule instantiation.
     */
    public final GrpcServerExtension directExecutor() {
        checkState(serverName == null, "directExecutor() can only be called at the rule instantiation");
        useDirectExecutor = true;
        return this;
    }

    /**
     * Returns a {@link ManagedChannel} connected to this service.
     */
    public final ManagedChannel getChannel() {
        return channel;
    }

    /**
     * Returns the underlying gRPC {@link Server} for this service.
     */
    public final Server getServer() {
        return server;
    }

    /**
     * Returns the randomly generated server name for this service.
     */
    public final String getServerName() {
        return serverName;
    }

    /**
     * Returns the service registry for this service. The registry is used to add service instances
     * (e.g. {@link BindableService} or {@link ServerServiceDefinition} to the server.
     */
    public final MutableHandlerRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * After the test has completed, clean up the channel and server.
     */
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        serverName = null;
        serviceRegistry = null;

        channel.shutdown();
        server.shutdown();

        try {
            channel.awaitTermination(1, TimeUnit.MINUTES);
            server.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            channel.shutdownNow();
            channel = null;

            server.shutdownNow();
            server = null;
        }
    }

    /**
     * Before the test has started, create the server and channel.
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        serverName = UUID.randomUUID().toString();

        serviceRegistry = new MutableHandlerRegistry();

        InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName)
          .fallbackHandlerRegistry(serviceRegistry);

        if (useDirectExecutor) {
            serverBuilder.directExecutor();
        }

        server = serverBuilder.build().start();

        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName);

        if (useDirectExecutor) {
            channelBuilder.directExecutor();
        }

        channel = channelBuilder.build();
    }
}
