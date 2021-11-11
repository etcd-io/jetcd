package io.etcd.jetcd.launcher;

import java.net.URI;
import java.util.List;

import org.testcontainers.lifecycle.Startable;

public interface EtcdCluster extends Startable {

    default void restart() {
        stop();
        start();
    }

    String clusterName();

    List<URI> clientEndpoints();

    List<URI> peerEndpoints();

    List<EtcdContainer> containers();
}
