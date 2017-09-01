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


ETCD_VERSION="v3.2.6"
OS=`uname`

if [[ $OS =~ ^Darwin ]]; then
  BINOS="darwin"
  SUFFIX=".zip"
elif  [[ $OS =~ ^Linux ]]; then
  BINOS="linux"
  SUFFIX=".tar.gz"
else
  echo "Unsupported OS: ${OS}"
  exit 1
fi

DIRNAME="etcd-${ETCD_VERSION}-${BINOS}-amd64"
BALL="${DIRNAME}${SUFFIX}"

mkdir -p external
cd external

if [[ ! -d "${DIRNAME}" ]]; then
  curl -L https://github.com/coreos/etcd/releases/download/${ETCD_VERSION}/${BALL} -o ${BALL}
  tar xzvf ${BALL}
else
  echo "skip to download since ${BALL} exists."
fi

cd ${DIRNAME}

./etcd --version

nohup bash -c './etcd --name infra1 --listen-client-urls http://127.0.0.1:12379 --advertise-client-urls http://127.0.0.1:12379 --listen-peer-urls http://127.0.0.1:12380 --initial-advertise-peer-urls http://127.0.0.1:12380 --initial-cluster-token etcd-cluster-1 --initial-cluster 'infra1=http://127.0.0.1:12380,infra2=http://127.0.0.1:22380,infra3=http://127.0.0.1:32380' --initial-cluster-state new --enable-pprof&'

nohup bash -c './etcd --name infra2 --listen-client-urls http://127.0.0.1:22379 --advertise-client-urls http://127.0.0.1:22379 --listen-peer-urls http://127.0.0.1:22380 --initial-advertise-peer-urls http://127.0.0.1:22380 --initial-cluster-token etcd-cluster-1 --initial-cluster 'infra1=http://127.0.0.1:12380,infra2=http://127.0.0.1:22380,infra3=http://127.0.0.1:32380' --initial-cluster-state new --enable-pprof&'

nohup bash -c './etcd --name infra3 --listen-client-urls http://127.0.0.1:32379 --advertise-client-urls http://127.0.0.1:32379 --listen-peer-urls http://127.0.0.1:32380 --initial-advertise-peer-urls http://127.0.0.1:32380 --initial-cluster-token etcd-cluster-1 --initial-cluster 'infra1=http://127.0.0.1:12380,infra2=http://127.0.0.1:22380,infra3=http://127.0.0.1:32380' --initial-cluster-state new --enable-pprof&'
