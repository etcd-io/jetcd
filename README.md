# jetcd - A Java Client for etcd
[![Build Status](https://github.com/etcd-io/jetcd/actions/workflows/build-main.yml/badge.svg)](https://github.com/etcd-io/jetcd/actions)
[![License](https://img.shields.io/badge/Licence-Apache%202.0-blue.svg?style=flat-square)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/io.etcd/jetcd-core.svg?style=flat-square)](https://search.maven.org/#search%7Cga%7C1%7Cio.etcd)
[![Javadocs](http://www.javadoc.io/badge/io/etcd/jetcd-core.svg)](https://javadoc.io/doc/io.etcd/jetcd-core)

jetcd is the official java client for [etcd](https://github.com/etcd-io/etcd) v3.

> Note: jetcd is work-in-progress and may break backward compatibility.

## Java Versions

Java 11 or above is required.

## Download

### Maven
```xml
<dependency>
  <groupId>io.etcd</groupId>
  <artifactId>jetcd-core</artifactId>
  <version>${jetcd-version}</version>
</dependency>
```

Development snapshots are available in [Sonatypes's snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/io/etcd/).

### Gradle

```
dependencies {
    implementation "io.etcd:jetcd-core:$jetcd-version"
}
```

### Usage

```java
// create client using endpoints
Client client = Client.builder().endpoints("http://etcd0:2379", "http://etcd1:2379", "http://etcd2:2379").build();
```

```java
// create client using target which enable using any name resolution mechanism provided
// by grpc-java (i.e. dns:///foo.bar.com:2379)
Client client = Client.builder().target("ip:///etcd0:2379,etcd1:2379,etcd2:2379").build();
```

```java
KV kvClient = client.getKVClient();
ByteSequence key = ByteSequence.from("test_key".getBytes());
ByteSequence value = ByteSequence.from("test_value".getBytes());

// put the key-value
kvClient.put(key, value).get();

// get the CompletableFuture
CompletableFuture<GetResponse> getFuture = kvClient.get(key);

// get the value from CompletableFuture
GetResponse response = getFuture.get();

// delete the key
kvClient.delete(key).get();
```
To build one ssl secured client, refer to [secured client config](docs/SslConfig.md).

For full etcd v3 API, pleases refer to the [official API documentation](https://etcd.io/docs/current/learning/api/).

### Examples

The [jetcd-ctl](https://github.com/etcd-io/jetcd/tree/master/jetcd-ctl) is a standalone projects that show usage of jetcd.

## Launcher

The `io.etcd:jetcd-test` offers a convenient utility to programmatically start & stop an isolated `etcd` server.  This can be very useful e.g. for integration testing, like so:

```java
import io.etcd.jetcd.Client;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

@RegisterExtension
public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
        .withNodes(1)
        .build();

Client client = Client.builder().endpoints(cluster.clientEndpoints()).build();
```

This launcher uses the Testcontainers framework.
For more info and prerequisites visit [testcontainers.org](https://www.testcontainers.org).

## Versioning

The project follows [Semantic Versioning](http://semver.org/).

The current major version is zero (0.y.z). Anything may change at any time. The public API should not be considered stable.

## Build from source

The project can be built with [Gradle](https://gradle.org/):

```
./gradlew compileJava
```

## Running tests

The project is tested against a three node `etcd` setup started with the Launcher (above) :

```sh
$ ./gradlew test
```

### Troubleshooting

It recommends building the project before running tests so that you have artifacts locally. It will solve some problems if the latest snapshot hasn't been uploaded or network issues.

## Contact

* Mailing list: [etcd-dev](https://groups.google.com/g/etcd-dev)

## Contributing

See [CONTRIBUTING](https://github.com/etcd-io/jetcd/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

## License

jetcd is under the Apache 2.0 license. See the [LICENSE](https://github.com/etcd-io/jetcd/blob/master/LICENSE) file for details.


