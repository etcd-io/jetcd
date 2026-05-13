# Overview

The Watch provide methods to watch on a key interval and cancel a watcher. If the watcher is disconnected on error, it will be resumed automatically.

# Goals

1. The watch client should process watch request, the watch client create watch request for client with key, option(revision, delete only, put only and end key) and register callback for watch request.

2. Notification, when watch client receive event from etcd server, it will should call registered callback.

3. Resume, when the watch client disconnect with etcd server, the etcd client should automatically resume all the watch requests with option( revision = last received revision + 1).

4. Cancel watch request, the etcd client should process watch cancellation and filter all the notification after cancellation request.

5. The watch client should be able to make a progress notify request that propagates the latest revision number to all watches.

# Implementation

The etcd client process watch request with [watch function](#watch-function), process notification with [processEvents function](#processevents-function), process resume with [resume function](#resume-function), process cancel with [cancelWatch function](#cancelwatch-function) and request progress with [requestProgress function](#requestProgress-function).

## watch function

Watch watches on a key interval.

1. Send create request to [requestStream](#requeststream-instance).
2. If the watch is create successfully, the `onCreate` will be called and the ListenableFuture task will be completed.
3. If the watch is slow or the required rev is compacted, the watch request might be canceled from the server-side and the `onCreateFailed` will be called.

## processEvents function

Process subscribe watch events.

1. If the watch id is not in the watchers map, scan it in the [cancelWatchers](#cancelwatchers) map.
2. if it exist in [cancelWatchers](#cancelwatchers), ignore, otherwise cancel it.
3. If the watcher exist in [watchers](#watchers-instance) map, call the `onWatch` and set the last revision for resume.

## resume function

1. Set requestStream as null, so getRequestStream will make new requestStream.
2. call [resumeWatchers](#resumewatchers-function) to resume all working watchers.

## cancelWatch function

Cancel the watch task with the watcher, the `onCanceled` will be called after successfully canceled.

1. The watcher will be removed from [watchers](#watchers-instance) map.
2. If the [watchers](#watchers-instance) map contain the watcher, it will be moved to [cancelWatchers](#cancelwatchers) and send cancel request to [requestStream](#requeststream-instance).

## requestProgress function

Send the latest revision processed to all active [watchers](#watchers-instance)

1. Send a progress request to [requestStream](#requeststream-instance).
2. Working watchers will receive a WatchResponse containing the latest revision number. All future revision numbers are guaranteed to be greater than or equal to the received revision number.

## requestStream instance

StreamObserver instance

1. It is created by gRPC call `watch`.
1. It will be a single instance and automatically created by [getRequestStream](#getrequeststream-function) if null.
2. `requestStream` is used to send request to etcd server for watch creation/cancel.
3. If error, this stream will be canceled by server and we need to resume this stream by set it to `null` and the [getRequestStream](#getrequeststream-function) will create a new one.

## Watcher Class

1. Hold callback for Watcher creation/cancel/resume/event.
2. Hold WatcherOption and key for resume.
3. Hold last revision for resume.

## watchers instance

ConcurrentHashMap collection for working watcher.

1. It is used for WatchResponse distribution.
2. It is used for resumes.

## pendingWatchers instance

It hold the on creating watchers.

## cancelWatchers

ConcurrentHashMap collection for canceling watcher.

1. It is used to filter canceled watch events response.
2. It is used to hold the canceling watcher.
3. The watcher will be deleted from `cancelWatchers` after canceled successfully.

## getRequestStream function

1. Single instance method to get [requestStream](#requeststream-instance).
2. Create requestStream with gRPC call `watch` with responseStream.
2. The responseStream will distribute the create, cancel, normal response to [processCreate](#processcreate-function), [processCanceled](#processcanceled-function) and [processEvents](#processevents-function).
3. If error happened, the [requestStream](#requeststream-instance) will be closed by server side, so we call resume to resume all ongoing watchers.

## processCreate function

Process create response from etcd server.

1. If there is no pendingWatcher, ignore.
2. If cancel flag is true or `CompactRevision!=0`  means the start revision has been compacted out of the store, call onCreateFailed.
3. If watchId = -1, create failed, call `onCreateFailed`.
4. If everything is Ok, create watcher, complete ListenableFuture task and put the new watcher to the [watchers](#watchers-instance) map.

## processCanceled function

Process cancel response from etcd server.

1. Remove the respond watcher from [cancelWatchers](#cancelwatchers).
2. call `onCancel` callback.


## resumeWatchers function

Resume all the the watchers on new requestStream.

1. Build new watch creation request for old watcher with last revision + 1.
2. Call `watch` function with the new watch creation request.

---

## jetcd: `WatchResponse` headers and parallel watchers

This section documents behavior of the **jetcd** `Watch` implementation (`io.etcd.jetcd.impl.WatchImpl`) and how it interacts with etcd’s gRPC API. It is useful when writing tests or comparing watch notifications.

### What etcd puts in `ResponseHeader.member_id`

In the etcd v3 API, every response carries a [`ResponseHeader`](https://etcd.io/docs/v3.6/learning/api/#response-header) that includes `member_id`: the ID of the **member that generated that RPC response**, not an identifier of the logical key or watch.

So two responses that describe the **same** store revision and the **same** `events` can still carry **different** `member_id` values if they were produced by **different** etcd members.

### One jetcd watcher ⇒ one gRPC `Watch` stream

In jetcd, each `Watch.watch(...)` returns a `Watcher` whose `resume()` opens **its own** bidirectional gRPC `Watch` call (`watchWithHandler` on the stub). There is **no** shared multiplexed `requestStream` for all watchers on the client (unlike the narrative elsewhere in this document, which follows the upstream Go client layout).

Practical consequence: **two** watchers on the same client are **two** independent streams.

### Multi-endpoint clients and load balancing

When the client is built with several endpoints (for example tests using `Client.builder().target("cluster://" + clusterName)`), gRPC uses a single `ManagedChannel` with a load-balancing policy over those addresses. Each **new** `Watch` RPC can be assigned to a **different** backend member.

So for two parallel watchers, it is normal that:

- `cluster_id`, `revision`, `raft_term`, and `events` match for the same logical update, but
- `header.member_id` (backed by protobuf `ResponseHeader.member_id`) **differs**, because each stream is answered by whichever member is serving that connection.

If both streams happen to land on the **same** member, `member_id` may match as well; relying on equality of full protobuf headers across watchers is therefore **flaky** on multi-member clusters.

### Recommendations

- When asserting that two watchers saw the “same” event, compare **events** (and revision-related fields you care about), not the entire raw header including `member_id`.
- Do not assume `member_id` identifies the watcher or the key; it identifies the **responding member** for that message only.
