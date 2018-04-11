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
$ ./gradlew clean check
 
 > Task :jetcd-core:test 
 
   com.coreos.jetcd.internal.impl.LeaseUnitTest
 
     ✔ testTimeToLiveNullOption
     ✔ testKeepAliveResetOnStreamErrors (1.6s)
     ✔ testKeepAliveOnce
     ✔ testKeepAliveCloseOnlyListener (1s)
     ✔ testKeepAliveOnSendingKeepAliveRequests
     - testKeepAliveReceivesExpiredLease
     ✔ testKeepAliveListenerClosesOnListening
     - testKeepAliveOnceStreamCloseOnSuccess
     ✔ testKeepAliveListenAfterListenerClose
     ✔ testKeepAliveClientClosesOnListening
     - testKeepAliveListenOnOneResponse
     ✔ testKeepAliveAfterFirstKeepAliveTimeout (7s)
     ✔ testKeepAliveCloseSomeListeners (1.2s)
     ✔ testKeepAliveOnceConnectError
 
   com.coreos.jetcd.internal.impl.MaintenanceUnitTest
 
     ✔ testWriteTwice
     ✔ testWriteAfterClosed
     ✔ testWrite
     ✔ testConnectionError
     ✔ testInterruptWrite
     ✔ testCloseWhenWrite
 
   com.coreos.jetcd.internal.impl.TxnResponseTest
 
     ✔ getTxnResponsesTest
     ✔ getDeleteResponsesTest
     ✔ getGetResponsesTest
     ✔ getPutResponsesTest
 
 
     09:12:30.317|INFO |org.testcontainers.dockerclient.DockerClientProviderStrategy - Loaded org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy from ~/.testcontainers.properties, will try it first
     09:12:30.956|INFO |org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy - Found docker client settings from environment
     09:12:30.975|INFO |org.testcontainers.dockerclient.DockerClientProviderStrategy - Found Docker environment with Environment variables, system properties and defaults. Resolved: 
         dockerHost=unix:///var/run/docker.sock
         apiVersion='{UNKNOWN_VERSION}'
         registryUrl='https://index.docker.io/v1/'
         registryUsername='lburgazz'
         registryPassword='null'
         registryEmail='null'
         dockerConfig='DefaultDockerClientConfig[dockerHost=unix:///var/run/docker.sock,registryUsername=lburgazz,registryPassword=<null>,registryEmail=<null>,registryUrl=https://index.docker.io/v1/,dockerConfig=/home/lburgazz/.docker,sslConfig=<null>,apiVersion={UNKNOWN_VERSION}[]'
     
     09:12:30.979|INFO |org.testcontainers.DockerClientFactory - Docker host IP address is localhost
     09:12:31.108|INFO |org.testcontainers.DockerClientFactory - Connected to docker: 
       Server Version: 1.13.1
       API Version: 1.26
       Operating System: Fedora 27 (Twenty Seven)
       Total Memory: 31612 MB
     09:12:31.807|INFO |org.testcontainers.DockerClientFactory - Ryuk started - will monitor and terminate Testcontainers containers on JVM exit
             ℹ︎ Checking the system...
             ✔ Docker version should be at least 1.6.0
             ✔ Docker environment should have more than 2GB free disk space
             ✔ File should be mountable
 
   com.coreos.jetcd.internal.impl.LoadBalancerTest
 
     ✔ testPickFirstBalancerFactory (1.3s)
     ✔ testRoundRobinLoadBalancerFactory
 
   com.coreos.jetcd.internal.impl.SslTest
 
     ✔ testSimpleSllSetup
 
   com.coreos.jetcd.internal.impl.WatchTest
 
     ✔ testWatchOnDelete
     ✔ testWatchOnPut
 
   com.coreos.jetcd.internal.impl.WatchUnitTest
 
     ✔ testCreateWatcherAfterClientClosed
     ✔ testWatcherCreateOnCompactionError
     ✔ testWatchOnUnrecoverableConnectionIssue
     ✔ testWatchOnSendingWatchCreateRequest
     ✔ testWatcherCreateOnInvalidWatchID
     ✔ testWatcherListenForMultiplePuts
     ✔ testWatcherCreateOnCancellationWithReason
     ✔ testWatcherListenAfterWatcherClose
     ✔ testWatcherCreateOnCancellationWithNoReason
     ✔ testWatchOnRecoverableConnectionIssue
     ✔ testWatcherListenOnResponse
     ✔ testWatcherListenOnWatchClientClose
     ✔ testWatcherDelete
     ✔ testWatcherListenOnWatcherClose
 
   com.coreos.jetcd.op.TxnTest
 
     ✔ testIfs
     ✔ testElses
     ✔ testThens
     ✔ testIfAfterElse
     ✔ testIfAfterThen
     ✔ testThenAfterElse
 
   46 passing (32.9s)
   3 pending
 
 
 > Task :jetcd-karaf:test 
 
 
     09:12:50.887|INFO |org.ops4j.pax.exam.spi.DefaultExamSystem - Pax Exam System (Version: 4.11.0) created.
     09:12:50.934|INFO |org.ops4j.pax.exam.junit.impl.ProbeRunner - creating PaxExam runner for class com.coreos.jetcd.osgi.ClientServiceTest
     09:12:50.961|INFO |org.ops4j.pax.exam.junit.impl.ProbeRunner - running test class com.coreos.jetcd.osgi.ClientServiceTest
     09:12:50.964|INFO |org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer - Creating RMI registry server on 127.0.0.1:21000
     09:12:51.689|INFO |org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer - Found 0 options when requesting OverrideJUnitBundlesOption.class
     09:12:51.702|INFO |org.ops4j.pax.exam.karaf.container.internal.KarafTestContainer - Wait for test container to finish its initialization [ RelativeTimeout value = 180000 []
     09:12:51.702|INFO |org.ops4j.pax.exam.rbc.client.RemoteBundleContextClient - Waiting for remote bundle context.. on 21000 name: 043cdd7f-d724-418d-bfc4-d460584817a7 timout: [ RelativeTimeout value = 180000 []
 
   com.coreos.jetcd.osgi.ClientServiceTest
 
     ✔ testServiceAvailability
 
     09:12:56.042|INFO |org.ops4j.pax.exam.spi.reactors.ReactorManager - suite finished
 
 
   1 passing (6.8s)
 
 
 > Task :jetcd-resolver:test 
 
   com.coreos.jetcd.resolver.NameResolverTest
 
     ✔ testDirectResolver
     ✔ testUnsupportedDirectSchema
     ✔ testDefaults
 
   3 passing (1.5s)
 
 
 > Task :jetcd-resolver-dns-srv:test 
 
   com.coreos.jetcd.resolver.dnssrv.NameResolverTest
 
     ✔ testUriResolverDiscovery
     ✔ testDnsSrvResolver
 
   2 passing (2s)
 
 
 
 BUILD SUCCESSFUL in 45s

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
