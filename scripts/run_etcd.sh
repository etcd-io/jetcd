#!/bin/bash

mkdir -p external
cd external

curl -L https://github.com/coreos/etcd/releases/download/v3.0.4/etcd-v3.0.4-linux-amd64.tar.gz -o etcd-v3.0.4-linux-amd64.tar.gz
tar xzvf etcd-v3.0.4-linux-amd64.tar.gz && cd etcd-v3.0.4-linux-amd64
./etcd --version

nohup bash -c './etcd &'