# Watch Implementation

## Overview

Each watcher manages its own dedicated gRPC stream with automatic reconnection.

## Architecture

```
WatchClient
  └── WatchConnection (per watcher)
        ├── WatchStateMachine    - Lifecycle management
        ├── WatchStream          - gRPC bidirectional stream
        ├── WatchResponseProcessor - Response parsing
        └── WatchReconnectionManager - Retry with backoff
```

## State Machine

States: `Connecting` → `Subscribing` → `Watching` ↔ `Reconnecting` → `Closed`

## Key Behaviors

1. **Watch creation** - Sends create request, waits for server confirmation
2. **Event processing** - Runs listener callbacks serially on Vert.x workers. The owning context requests one message at a time and waits for the listener after assembling a response.
3. **Auto-resume** - On disconnect, waits for an in-flight callback to return before choosing the resume revision.
4. **Cancellation** - Closes the transport independently of listener callbacks. The completion callback follows earlier notifications.
5. **Progress notify** - Propagates latest revision to all watchers

## Response headers

Each watch stream can be routed to a different etcd member when the client has
multiple endpoints. The response header's `member_id` identifies the member
that sent the response. Two watchers can therefore receive the same events at
the same store revision with different member IDs. When comparing their
notifications, compare the events and the revision fields your test needs.

## Usage

```java
Watch.Watcher watcher = client.getWatchClient().watch(
    ByteSequence.from("/prefix"),
    WatchOption.builder().isPrefix(true).build(),
    new Watch.Listener() {
        @Override
        public void onNext(WatchResponse response) {
            response.getEvents().forEach(event -> {
                System.out.println(event.getKeyValue().getKey());
            });
        }

        @Override
        public void onError(Throwable throwable) {
            // Handle error
        }

        @Override
        public void onCompleted() {
            // Watch closed
        }
    });

// Later
watcher.close();
```

## Reconnection

- Exponential backoff on failures
- Auth token refresh on token errors
