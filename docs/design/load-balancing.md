# Load Balancing

jetcd supports client-side load balancing via Vert.x's native load balancing.

## Strategies

| Strategy | Description |
|----------|-------------|
| **ROUND_ROBIN** | Distributes requests evenly (default) |
| **LEAST_REQUESTS** | Routes to endpoint with fewest active requests |
| **RANDOM** | Random endpoint selection |
| **POWER_OF_TWO_CHOICES** | Picks better of two random endpoints |

## Configuration

### Default (ROUND_ROBIN)

```java
Client client = Client.builder("http://etcd1:2379", "http://etcd2:2379", "http://etcd3:2379")
    .build();
```

### Explicit Strategy

```java
import io.vertx.core.net.endpoint.LoadBalancer;

Client client = Client.builder("http://etcd1:2379", "http://etcd2:2379", "http://etcd3:2379")
    .loadBalancer(LoadBalancer.LEAST_REQUESTS)
    .build();
```

### With DNS SRV

```java
Client client = Client.builder(ServiceResolvers.dnsSrv("_etcd._tcp.example.com"))
    .loadBalancer(LoadBalancer.POWER_OF_TWO_CHOICES)
    .build();
```

## Strategy Selection

- **ROUND_ROBIN** - Default, good for homogeneous clusters
- **LEAST_REQUESTS** - Best for heterogeneous clusters or varied request durations
- **RANDOM** - Minimal overhead, good for high-throughput scenarios
- **POWER_OF_TWO_CHOICES** - Balance between simplicity and load awareness

## Custom Load Balancer

```java
LoadBalancer custom = endpoints -> {
    // Custom selection logic
    return endpoints.get(0);
};

Client client = Client.builder("http://etcd1:2379", "http://etcd2:2379")
    .loadBalancer(custom)
    .build();
```
