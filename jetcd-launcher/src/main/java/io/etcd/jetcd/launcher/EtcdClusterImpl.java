package io.etcd.jetcd.launcher;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.testcontainers.containers.Network;
import org.testcontainers.utility.ResourceReaper;

import static java.util.stream.Collectors.toList;

public class EtcdClusterImpl implements EtcdCluster {
    private final List<EtcdContainer> containers;
    private final String clusterName;
    private final Network network;
    private final List<String> endpoints;

    public EtcdClusterImpl(
        String image,
        String clusterName,
        String prefix,
        int nodes,
        boolean ssl,
        Collection<String> additionalArgs,
        Network network) {

        this.clusterName = clusterName;
        this.network = network;
        this.endpoints = IntStream.range(0, nodes)
            .mapToObj(i -> (prefix == null ? "etcd" : prefix + "etcd") + i)
            .collect(toList());

        this.containers = endpoints.stream()
            .map(e -> {
                EtcdContainer answer = new EtcdContainer(image, e, endpoints)
                    .withClusterToken(clusterName)
                    .withSll(ssl)
                    .withAdditionalArgs(additionalArgs)
                    .withNetwork(network);

                return answer;
            })
            .collect(toList());
    }

    @Override
    public void start() {
        final CountDownLatch latch = new CountDownLatch(containers.size());
        final AtomicReference<Exception> failedToStart = new AtomicReference<>();

        ResourceReaper.instance().registerNetworkIdForCleanup(this.network.getId());

        for (EtcdContainer container : containers) {
            new Thread(() -> {
                try {
                    container.start();

                    ResourceReaper.instance().registerContainerForCleanup(
                        container.getContainerId(),
                        container.getDockerImageName());
                } catch (Exception e) {
                    failedToStart.set(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (failedToStart.get() != null) {
            throw new IllegalStateException("Cluster failed to start", failedToStart.get());
        }
    }

    @Override
    public void stop() {
        for (EtcdContainer container : containers) {
            container.stop();
        }
    }

    @Override
    public void close() {
        for (EtcdContainer container : containers) {
            container.close();
        }
    }

    @Override
    public String clusterName() {
        return clusterName;
    }

    @Override
    public List<URI> clientEndpoints() {
        return containers.stream().map(EtcdContainer::clientEndpoint).collect(Collectors.toList());
    }

    @Override
    public List<URI> peerEndpoints() {
        return containers.stream().map(EtcdContainer::peerEndpoint).collect(Collectors.toList());
    }

    @Override
    public List<EtcdContainer> containers() {
        return Collections.unmodifiableList(containers);
    }
}
