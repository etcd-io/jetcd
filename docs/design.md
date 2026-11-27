# jetcd Design

jetcd is the official Java client for etcd v3, built on Vert.x gRPC with async-first design.

## Architecture

```
Client
  └── GrpcService
        ├── GrpcClient (base)
        ├── GrpcClient (authenticated)
        └── ServiceResolver<SocketAddress>

Client Services: KV, Watch, Lease, Auth, Cluster, Maintenance, Lock, Election
```

## Design Documents

- **[API Design](design/api.md)** - ByteSequence vs String patterns, type safety
- **[Watch](design/watch.md)** - Watch implementation with auto-reconnect
- **[SSL/TLS](design/ssl.md)** - Secure client configuration
- **[DNS SRV](design/dns-srv.md)** - Dynamic service discovery
- **[Load Balancing](design/load-balancing.md)** - Client-side load balancing strategies

## Key Design Decisions

### Async-First with CompletableFuture

All operations return `CompletableFuture` for non-blocking usage. Internal implementation uses Vert.x futures, converted at API boundaries.

### Lazy Initialization

Client services (KV, Watch, etc.) are created on first access via memoizing suppliers, avoiding unnecessary resource allocation.

### Type-Safe Service Resolution

`ServiceResolver<SocketAddress>` provides strongly-typed endpoint resolution with support for static endpoints and DNS SRV discovery.

### Retry with Failsafe

Retry policies use exponential backoff and can be configured per client. Concurrent
requests share one authentication call per client. An authentication failure
invalidates the cached token only if the failing request used that cache entry.
Unary calls use etcd's gRPC status and message to identify invalid tokens and stale
auth revisions. Only calls eligible for transport retries can be repeated after
`UNAVAILABLE` or a connection failure. KV writes require an explicit `autoRetry`
opt-in for those retries.
Lease grants retry only after a confirmed token rejection. A timed grant uses one
deadline for authentication, backoff, and the grant RPC.

Unary RPCs retain the server's status and message until the client maps the
failure. Election calls use the explicit etcd messages for no leader and not
leader and surface other server failures with their original status and cause.
