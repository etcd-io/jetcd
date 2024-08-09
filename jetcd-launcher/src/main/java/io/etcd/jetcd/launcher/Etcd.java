/*
 * Copyright 2016-2021 The jetcd authors
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

package io.etcd.jetcd.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.testcontainers.containers.Network;

import com.google.common.base.Strings;

public final class Etcd {
    public static final String CONTAINER_IMAGE = "gquay.io/coreos/etcd:v3.5.14";
    public static final int ETCD_CLIENT_PORT = 2379;
    public static final int ETCD_PEER_PORT = 2380;
    public static final String ETCD_DATA_DIR = "/data.etcd";

    private Etcd() {
    }

    private static String resolveContainerImage() {
        String image = System.getenv("ETCD_IMAGE");
        if (!Strings.isNullOrEmpty(image)) {
            return image;
        }
        return CONTAINER_IMAGE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String image = Etcd.resolveContainerImage();
        private String clusterName = UUID.randomUUID().toString();
        private String prefix;
        private int nodes = 1;
        private boolean ssl = false;
        private boolean debug = false;
        private List<String> additionalArgs;
        private Network network;
        private boolean shouldMountDataDirectory = true;
        private String user;

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

        public Builder withDebug(boolean debug) {
            this.debug = debug;
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
                debug,
                additionalArgs,
                network != null ? network : Network.SHARED,
                shouldMountDataDirectory,
                user);
        }

        public Builder withMountedDataDirectory(boolean shouldMountDataDirectory) {
            this.shouldMountDataDirectory = shouldMountDataDirectory;
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }
    }
}
