# DNS SRV Resolution

jetcd supports DNS SRV records for dynamic endpoint discovery.

## Usage

### Default DNS Server

```java
Client client = Client.builder(ServiceResolvers.dnsSrv("_etcd._tcp.example.com"))
    .build();
```

### Custom DNS Server

```java
Client client = Client.builder(ServiceResolvers.dnsSrv(
        "_etcd._tcp.example.com",
        "dns.internal.example.com",
        53))
    .build();
```

## DNS SRV Record Setup

```dns
; SRV records
_etcd._tcp.example.com. 300 IN SRV 0 0 2379 etcd-1.example.com.
_etcd._tcp.example.com. 300 IN SRV 0 0 2379 etcd-2.example.com.
_etcd._tcp.example.com. 300 IN SRV 0 0 2379 etcd-3.example.com.

; A records
etcd-1.example.com. 300 IN A 10.0.1.10
etcd-2.example.com. 300 IN A 10.0.1.11
etcd-3.example.com. 300 IN A 10.0.1.12
```

Verify with: `dig SRV _etcd._tcp.example.com`

## Kubernetes

With a headless service, Kubernetes creates DNS SRV records automatically:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: etcd
spec:
  clusterIP: None  # Headless
  ports:
  - name: etcd-client
    port: 2379
  selector:
    app: etcd
```

```java
Client client = Client.builder(
        ServiceResolvers.dnsSrv("_etcd-client._tcp.etcd.default.svc.cluster.local"))
    .build();
```

## Static vs DNS SRV

| Approach | Pros | Cons |
|----------|------|------|
| Static endpoints | Simple, no DNS dependency | Manual updates when servers change |
| DNS SRV | Dynamic discovery, no code changes | Requires DNS infrastructure |
