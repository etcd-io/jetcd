#!/usr/bin/env bash
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

export SCRIPT_PATH=$(dirname "$(readlink -f "$0")")
export ROOT=$SCRIPT_PATH
export CFSSL_HOME=$ROOT/cfssl
export CERT_HOME=$ROOT/cert/

mkdir -p $CFSSL_HOME
mkdir -p $CERT_HOME

curl -s -L -o $CFSSL_HOME/cfssl https://pkg.cfssl.org/R1.2/cfssl_linux-amd64
curl -s -L -o $CFSSL_HOME/cfssljson https://pkg.cfssl.org/R1.2/cfssljson_linux-amd64
chmod +x $CFSSL_HOME/{cfssl,cfssljson}


cd $CERT_HOME

echo '{"CN":"CA","key":{"algo":"rsa","size":2048}}' | $CFSSL_HOME/cfssl gencert -initca - | $CFSSL_HOME/cfssljson -bare ca -
echo '{"signing":{"default":{"expiry":"43800h","usages":["signing","key encipherment","server auth","client auth"]}}}' > ca-config.json

export ADDRESS=etcd-ssl
export NAME=server
echo '{"CN":"'$NAME'","hosts":[""],"key":{"algo":"rsa","size":2048}}' | $CFSSL_HOME/cfssl gencert -config=ca-config.json -ca=ca.pem -ca-key=ca-key.pem -hostname="$ADDRESS" - | $CFSSL_HOME/cfssljson -bare $NAME

export ADDRESS=
export NAME=client
echo '{"CN":"'$NAME'","hosts":[""],"key":{"algo":"rsa","size":2048}}' | $CFSSL_HOME/cfssl gencert -config=ca-config.json -ca=ca.pem -ca-key=ca-key.pem -hostname="$ADDRESS" - | $CFSSL_HOME/cfssljson -bare $NAME
