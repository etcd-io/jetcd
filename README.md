# jetcd - A Java Client for etcd

[![Build Status](https://github.com/etcd-io/jetcd/actions/workflows/build-main.yml/badge.svg)](https://github.com/etcd-io/jetcd/actions)
[![License](https://img.shields.io/badge/Licence-Apache%202.0-blue.svg?style=flat-square)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/io.etcd/jetcd-core.svg?style=flat-square)](https://search.maven.org/#search%7Cga%7C1%7Cio.etcd)
[![Javadocs](http://www.javadoc.io/badge/io/etcd/jetcd-core.svg)](https://javadoc.io/doc/io.etcd/jetcd-core)

jetcd is the official Java client for [etcd](https://github.com/etcd-io/etcd) v3.

> Note: jetcd is work-in-progress and may break backward compatibility.

## Requirements

Java 21 or above.

For upgrades from grpc-java-based versions, see the [upgrade guide](docs/upgrade.md).

## Installation

### Maven

```xml
<dependency>
  <groupId>io.etcd</groupId>
  <artifactId>jetcd-core</artifactId>
  <version>${jetcd-version}</version>
</dependency>
```

### Gradle

```groovy
implementation "io.etcd:jetcd-core:$jetcdVersion"
```

## Quick Start

```java
// Create client
Client client = Client.builder("http://etcd0:2379", "http://etcd1:2379", "http://etcd2:2379")
    .build();

// KV operations
KV kvClient = client.getKVClient();
ByteSequence key = ByteSequence.from("test_key", StandardCharsets.UTF_8);
ByteSequence value = ByteSequence.from("test_value", StandardCharsets.UTF_8);

kvClient.put(key, value).get();
GetResponse response = kvClient.get(key).get();
kvClient.delete(key).get();

client.close();
```

## Documentation

For detailed documentation, see **[docs/design.md](docs/design.md)**.

Key topics:
- [API Design](docs/design/api.md) - ByteSequence vs String patterns
- [SSL/TLS Configuration](docs/design/ssl.md) - Secure client setup
- [DNS SRV Resolution](docs/design/dns-srv.md) - Dynamic endpoint discovery
- [Load Balancing](docs/design/load-balancing.md) - Client-side load balancing

For etcd v3 API reference, see the [official etcd documentation](https://etcd.io/docs/current/learning/api/).

## Testing

The `io.etcd:jetcd-test` module provides utilities for integration testing:

```java
import org.junit.jupiter.api.extension.RegisterExtension;

import io.etcd.jetcd.test.EtcdClusterExtension;

@RegisterExtension
public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
    .withNodes(1)
    .build();

Client client = Client.builder(cluster.clientEndpoints()).build();
```

Uses [Testcontainers](https://www.testcontainers.org) - see their docs for prerequisites.

## Building

```bash
./gradlew compileJava
./gradlew test
```

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.

## License

Apache 2.0 - see [LICENSE](LICENSE).
