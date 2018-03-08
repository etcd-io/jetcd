# jetcd - A Java Client for etcd
[![Build Status](https://img.shields.io/travis/coreos/jetcd/master.svg?style=flat-square)](https://travis-ci.org/coreos/jetcd)
[![License](https://img.shields.io/badge/Licence-Apache%202.0-blue.svg?style=flat-square)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/com/coreos/jetcd-core.svg?style=flat-square)](https://search.maven.org/#search%7Cga%7C1%7Ccoreos)
[![GitHub release](https://img.shields.io/github/release/coreos/jetcd.svg?style=flat-square)](https://github.com/coreos/jetcd/releases)

jetcd is the official java client for [etcd](https://github.com/coreos/etcd)v3.

> Note: jetcd is work-in-progress and may break backward compatibility.

## Java Versions

Java 8 or above is required.

## Download

### Maven
```xml
<dependency>
  <groupId>com.coreos</groupId>
  <artifactId>jetcd-core</artifactId>
  <version>0.0.2</version>
</dependency>
```

Development snapshots are available in [Sonatypes's snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/).

### Gradle

```
dependencies {
    compile 'com.coreos:jetcd-core:0.0.2'
}
``` 

### Manual

Download latest jetcd-core jar from [Maven](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.coreos%22%20AND%20v%3A%220.0.2%22) and all its dependent jars:

[grpc-core-1.10.0.jar](http://repo1.maven.org/maven2/io/grpc/grpc-core/1.10.0/)
[grpc-netty-1.10.0.jar](http://repo1.maven.org/maven2/io/grpc/grpc-netty/1.10.0/)
[grpc-protobuf-1.10.0.jar](http://repo1.maven.org/maven2/io/grpc/grpc-protobuf/1.10.0/)
[grpc-stub-1.10.0.jar](http://repo1.maven.org/maven2/io/grpc/grpc-stub/1.10.0/)
[slf4j-api-1.7.2.jar](http://repo1.maven.org/maven2/org/apache/directory/studio/org.slf4j.api/1.7.2/)

### Usage

```java
// create client
Client client = Client.builder().endpoints("http://localhost:2379").build();
KV kvClient = client.getKVClient();

ByteSequence key = ByteSequence.fromString("test_key");
ByteSequence value = ByteSequence.fromString("test_value");

// put the key-value
kvClient.put(key, value).get();
// get the CompletableFuture
CompletableFuture<GetResponse> getFuture = kvClient.get(key);
// get the value from CompletableFuture
GetResponse response = getFuture.get();
// delete the key
DeleteResponse deleteRangeResponse = kvClient.delete(key).get();
```

For full etcd v3 API, plesase refer to [API_Reference](https://github.com/coreos/etcd/blob/master/Documentation/dev-guide/api_reference_v3.md).

### Examples

The [examples](https://github.com/coreos/jetcd/tree/master/jetcd-examples) are standalone projects that show usage of jetcd.

## Versioning

The project follows [Semantic Versioning](http://semver.org/).

The current major version is zero (0.y.z). Anything may change at any time. The public API should not be considered stable.

## Running tests

The project is to be tested against a three node `etcd` setup, which automatically launched via Testcontainers framework.
For more info and prerequisites visit [official website](https://www.testcontainers.org/)
It should work on either macOS or Linux.

```sh
$ mvn test
...

[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running TestSuite
[WARNING] Tests run: 104, Failures: 0, Errors: 0, Skipped: 3, Time elapsed: 31.308 s - in TestSuite
[INFO]
[INFO] Results:
[INFO]
[WARNING] Tests run: 104, Failures: 0, Errors: 0, Skipped: 3
...
[INFO] Reactor Summary:
[INFO]
[INFO] jetcd .............................................. SUCCESS [  0.010 s]
[INFO] jetcd-core ......................................... SUCCESS [ 55.480 s]
[INFO] jetcd-discovery-dns-srv ............................ SUCCESS [  3.225 s]
[INFO] jetcd-watch-example ................................ SUCCESS [  0.291 s]
[INFO] jetcd-simple-ctl ................................... SUCCESS [  0.028 s]
[INFO] jetcd-examples ..................................... SUCCESS [  0.000 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 59.929 s
[INFO] Finished at: 2018-02-13T12:51:13-08:00
[INFO] Final Memory: 84M/443M
````

## Contact

* Mailing list: [etcd-dev](https://groups.google.com/forum/?hl=en#!forum/etcd-dev)
* IRC: #[etcd](irc://irc.freenode.org:6667/#etcd) on freenode.org

## Contributing

See [CONTRIBUTING](https://github.com/coreos/jetcd/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

## Reporting bugs

See [reporting bugs](https://github.com/coreos/etcd/blob/master/Documentation/reporting_bugs.md) for details about reporting any issues.

## License

jetcd is under the Apache 2.0 license. See the [LICENSE](https://github.com/coreos/jetcd/blob/master/LICENSE) file for details.
