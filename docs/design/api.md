# API Design

## ByteSequence vs String

The jetcd API uses two types for text/binary data:

- **`ByteSequence`** - Data stored in etcd (keys, values, user/role names)
- **`String`** - Configuration/infrastructure (endpoints, DNS names, headers)

### Why ByteSequence for Data?

1. **Binary safety** - etcd keys/values can contain any bytes, not just UTF-8
2. **Explicit encoding** - No platform-dependent charset defaults
3. **Performance** - Zero-copy protobuf integration, cached hash codes
4. **Immutability** - Thread-safe by design

### Why String for Configuration?

1. **Semantic correctness** - URLs, DNS names, HTTP headers are text by specification
2. **Ergonomics** - No unnecessary conversions for simple config
3. **Standards compliance** - HTTP/DNS are string-based protocols

## Usage Summary

| Operation Type | Parameter Type | Rationale |
|---------------|----------------|-----------|
| KV, Auth, Watch, Lock, Election | `ByteSequence` | Data stored in etcd |
| Maintenance endpoints | `String` | Network addresses |
| Service resolution | `String` | URLs and DNS names |
| HTTP headers | `String` | HTTP spec defines as strings |

## Examples

### Data Operations (ByteSequence)

```java
ByteSequence key = ByteSequence.from("/config/timeout");
ByteSequence value = ByteSequence.from("30");
kv.put(key, value).get();

// Binary data
ByteSequence binaryKey = ByteSequence.from(new byte[]{0x01, 0x02, 0x03});
```

### Configuration (String)

```java
// Endpoints
Client client = Client.builder("http://etcd1:2379", "http://etcd2:2379").build();

// Maintenance operations
maintenance.statusMember("http://etcd-1:2379").get();

// DNS SRV
ServiceResolver resolver = ServiceResolvers.dnsSrv("_etcd._tcp.example.com");
```

## Conversion

```java
// String to ByteSequence
ByteSequence key = ByteSequence.from("mykey");
ByteSequence key = ByteSequence.from("mykey", StandardCharsets.UTF_8);

// ByteSequence to String
String str = key.toString(StandardCharsets.UTF_8);

// ByteSequence to bytes
byte[] bytes = key.getBytes();
```
