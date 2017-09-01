#!/bin/bash
#
# Copyright 2017 The jetcd authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

ETCD_VERSION="v3.2"

docker network ls | grep etcd 2>&1 > /dev/null
if [ $? -ne 0 ]; then
    docker network create etcd
fi

# Pull docker
docker pull gcr.io/etcd-development/etcd:${ETCD_VERSION}

docker run \
    --detach \
    --rm \
    --name etcd1 \
    --network etcd \
    --publish 12379:2379 \
    gcr.io/etcd-development/etcd:${ETCD_VERSION} \
    etcd \
        -name etcd1 \
        -advertise-client-urls "http://127.0.0.1:2379" \
        -listen-client-urls "http://0.0.0.0:2379" \
        -initial-advertise-peer-urls http://etcd1:2380 \
        -listen-peer-urls http://0.0.0.0:2380 \
        -initial-cluster-token etcd-cluster \
        -initial-cluster etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380 \
        -initial-cluster-state new

docker run \
    --detach \
    --rm \
    --name etcd2 \
    --network etcd \
    --publish 22379:2379 \
    gcr.io/etcd-development/etcd:${ETCD_VERSION} \
    etcd \
        -name etcd2 \
        -advertise-client-urls "http://127.0.0.1:2379" \
        -listen-client-urls "http://0.0.0.0:2379" \
        -initial-advertise-peer-urls http://etcd2:2380 \
        -listen-peer-urls http://0.0.0.0:2380 \
        -initial-cluster-token etcd-cluster \
        -initial-cluster etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380 \
        -initial-cluster-state new


docker run \
    --detach \
    --rm \
    --name etcd3 \
    --network etcd \
    --publish 32379:2379 \
    gcr.io/etcd-development/etcd:${ETCD_VERSION} \
    etcd \
        -name etcd3 \
        -advertise-client-urls "http://127.0.0.1:2379" \
        -listen-client-urls "http://0.0.0.0:2379" \
        -initial-advertise-peer-urls http://etcd3:2380 \
        -listen-peer-urls http://0.0.0.0:2380 \
        -initial-cluster-token etcd-cluster \
        -initial-cluster etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380 \
        -initial-cluster-state new

docker run \
    --detach \
    --rm \
    --name etcd-proxy \
    --network etcd \
    --publish 2379:2379 \
    gcr.io/etcd-development/etcd:${ETCD_VERSION} \
    etcd \
        grpc-proxy \
        start \
        --endpoints=etcd1:2379,etcd2:2379,etcd3:2379 \
        --listen-addr=0.0.0.0:2379
