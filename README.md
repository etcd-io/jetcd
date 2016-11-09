# jetcd

[![Build Status](https://travis-ci.org/coreos/jetcd.svg?branch=master)](https://travis-ci.org/coreos/jetcd)

Java client for etcd v3.

## Getting started

### Usage

```
EtcdClient client = EtcdClientBuilder.newBuilder().endpoints("http://localhost:2379").build();
EtcdKV kvClient = client.getKVClient();

ByteString key = ByteString.copyFrom("test_key", "UTF-8");
ByteString value = ByteString.copyFrom("test_value", "UTF-8");

// put the key-value
kvClient.put(key, value).get();
// get the value
ListenableFuture<RangeResponse> getFeature = kvClient.get(key);
RangeResponse response = getFeature.get();
assertEquals(response.getKvsCount(), 1);
assertEquals(response.getKvs(0).getValue().toStringUtf8(), "test_value");
// delete the key
kvClient.delete(key).get()
```

For full etcd v3 API, plesase refer to [API_Reference](https://github.com/coreos/etcd/blob/master/Documentation/dev-guide/api_reference_v3.md).

## Building

### Gradle

```gradle clean build```

Will produce a usable artifact (JAR) and will run all integration tests.

The tests rely on a usable environment, and will, by default, pull down the
etcd Docker image from [Quay.io](https://quay.io/repository/coreos/etcd), and
start containers as needed.

Therefore, in order to build, you will need Docker (recommended), or running
instances of etcd.

Follow the documentation to install (Docker here)[http://www.docker.com/products/docker].

Otherwise, the tests rely on a single instance running, and a cluster of three (3)
instances.

You can pass in the instance endpoints like so:

```gradle -DSINGLE_ENDPOINT=http://localhost:2379 -DCLUSTER_ENDPOINTS=http://localhost:12379,http://localhost:22379,http://localhost:32379 clean build```

## Contact

* Mailing list: [etcd-dev](https://groups.google.com/forum/?hl=en#!forum/etcd-dev)

## License

jetcd is under the Apache 2.0 license. See the [LICENSE](https://github.com/coreos/jetcd/blob/master/LICENSE) file for details.
