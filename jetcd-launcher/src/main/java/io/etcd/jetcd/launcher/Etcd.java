package io.etcd.jetcd.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.testcontainers.containers.Network;

public final class Etcd {
    public static final String CONTAINER_IMAGE = "gcr.io/etcd-development/etcd:v3.4.7";
    public static final int ETCD_CLIENT_PORT = 2379;
    public static final int ETCD_PEER_PORT = 2380;
    public static final String ETCD_DATA_DIR = "/data.etcd";

    private Etcd() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String image = Etcd.CONTAINER_IMAGE;
        private String clusterName = UUID.randomUUID().toString();
        private String prefix;
        private int nodes = 1;
        private boolean ssl = false;
        private List<String> additionalArgs;
        private Network network;

        public Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public Builder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder withNodes(int nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder withSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public Builder withAdditionalArgs(Collection<String> additionalArgs) {
            this.additionalArgs = Collections.unmodifiableList(new ArrayList<>(additionalArgs));
            return this;
        }

        public Builder withAdditionalArgs(String... additionalArgs) {
            this.additionalArgs = Collections.unmodifiableList(Arrays.asList(additionalArgs));
            return this;
        }

        public Builder withImage(String image) {
            this.image = image;
            return this;
        }

        public Builder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public EtcdCluster build() {
            return new EtcdClusterImpl(
                image,
                clusterName,
                prefix,
                nodes,
                ssl,
                additionalArgs,
                network != null ? network : Network.newNetwork());
        }
    }
}
