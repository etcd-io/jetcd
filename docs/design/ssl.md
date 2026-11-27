# SSL/TLS Configuration

jetcd uses Vert.x's `HttpClientOptions` for SSL/TLS. The `io.etcd.jetcd.common.vertx.Ssl` class provides helpers.

## CA Trust Only

```java
Client client = Client.builder("https://etcd:2379")
    .httpClientOptions(Ssl.withTrustManager("/etc/etcd/pki/ca.crt"))
    .build();
```

Or with `File` or `InputStream`:

```java
File caCert = new File("/etc/etcd/pki/ca.crt");
Client client = Client.builder("https://etcd:2379")
    .httpClientOptions(Ssl.withTrustManager(caCert))
    .build();
```

## mTLS (Client Certificate)

```java
Client client = Client.builder("https://etcd:2379")
    .httpClientOptions(Ssl.withTrustAndKeyManager(
        "/etc/etcd/pki/ca.crt",
        "/etc/etcd/pki/client.crt",
        "/etc/etcd/pki/client.key"))
    .build();
```

## Consumer Composition

Chain additional options with `.andThen()`:

```java
Client client = Client.builder("https://etcd:2379")
    .httpClientOptions(Ssl.withTrustManager("/path/to/ca.crt")
        .andThen(options -> options
            .setConnectTimeout(5000)
            .setIdleTimeout(60)))
    .build();
```

## Direct HttpClientOptions

For full control:

```java
HttpClientOptions options = new HttpClientOptions()
    .setSsl(true)
    .setUseAlpn(true)
    .setTrustOptions(new PemTrustOptions().addCertPath("/path/to/ca.crt"))
    .setVerifyHost(true);

Client client = Client.builder("https://etcd:2379")
    .httpClientOptions(options)
    .build();
```

## Key Options

- `setSsl(true)` - Enable SSL/TLS
- `setUseAlpn(true)` - Enable ALPN for HTTP/2 (required for gRPC)
- `setTrustOptions()` - CA certificates
- `setKeyCertOptions()` - Client certificate for mTLS
- `setVerifyHost()` - Hostname verification (default: true)
