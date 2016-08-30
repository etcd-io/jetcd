package com.coreos.jetcd;

import com.coreos.jetcd.api.*;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.util.ListenableSetFuture;
import com.coreos.jetcd.watch.Watcher;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * etcd watcher Implementation
 */
public class EtcdWatchImpl implements EtcdWatch {

    private StreamObserver<WatchRequest> requestStream;

    private ConcurrentHashMap<Long, Watcher> watchers = new ConcurrentHashMap<>();

    private WatchGrpc.WatchStub watchStub;

    private ConcurrentLinkedQueue<Pair<Watcher.Builder, ListenableSetFuture<Watcher>>> pendingWatchers = new ConcurrentLinkedQueue<>();
    private Map<Long, Watcher> cancelWatchers = new ConcurrentHashMap<>();

    public EtcdWatchImpl(WatchGrpc.WatchStub watchStub) {
        this.watchStub = watchStub;
    }

    /**
     * Watch watches on a key or prefix. The watched events will be called by onWatch.
     * If the watch is slow or the required rev is compacted, the watch request
     * might be canceled from the server-side and the onCreateFailed will be called.
     *
     * @param key         the key subscribe
     * @param watchOption key option
     * @param callback    call back
     * @return ListenableFuture watcher
     */
    @Override
    public synchronized ListenableSetFuture<Watcher> watch(ByteString key, WatchOption watchOption, Watcher.WatchCallback callback) {
        WatchRequest request = optionToWatchCreateRequest(key, watchOption);
        Watcher.Builder builder = Watcher.newBuilder().withCallBack(callback)
                .withKey(key)
                .withWatchOption(watchOption);
        ListenableSetFuture<Watcher> waitFuture = new ListenableSetFuture<>(null);
        this.pendingWatchers.add(new Pair<>(builder, waitFuture));
        getRequestStream().onNext(request);
        return waitFuture;
    }

    /**
     * Cancel the watch task with the watcher, the onCanceled will be called after successfully canceled.
     *
     * @param watcher the watcher to be canceled
     */
    @Override
    public void cancelWatch(Watcher watcher) {
        Watcher temp = watchers.get(watcher.getWatchID());
        if (temp != null) {
            synchronized (temp) {
                if (this.watchers.containsKey(temp.getWatchID())) {
                    this.watchers.remove(temp.getWatchID());
                    this.cancelWatchers.put(temp.getWatchID(), temp);
                    WatchCancelRequest cancelRequest = WatchCancelRequest.newBuilder().setWatchId(watcher.getWatchID()).build();
                    WatchRequest request = WatchRequest.newBuilder().setCancelRequest(cancelRequest).build();
                    this.requestStream.onNext(request);
                }
            }
        }
    }

    /**
     * empty the old request stream, watchers and resume the old watchers
     * empty the cancelWatchers as there is no need to cancel, the old request stream has been dead
     */
    private void resume() {
        synchronized (this) {
            this.requestStream = null;
            Watcher[] resumeWatchers = (Watcher[]) watchers.values().toArray();
            this.watchers.clear();
            this.cancelWatchers.clear();
            resumeWatchers(resumeWatchers);
        }

    }

    /**
     * single instance method to get request stream, empty the old requestStream, so we will get a new
     * requestStream automatically
     * <p>the responseStream will distribute the create, cancel, normal response to
     * processCreate, processCanceled and processEvents
     * <p>if error happened, the requestStream will be closed by server side, so we call resume to resume
     * all ongoing watchers
     *
     * @return
     */
    private StreamObserver<WatchRequest> getRequestStream() {
        if (this.requestStream == null) {
            synchronized (this) {
                if (this.requestStream == null) {
                    StreamObserver<WatchResponse> watchResponseStreamObserver = new StreamObserver<WatchResponse>() {
                        @Override
                        public void onNext(WatchResponse watchResponse) {
                            if (watchResponse.getCreated()) {
                                processCreate(watchResponse);
                            } else if (watchResponse.getCanceled()) {
                                processCanceled(watchResponse);
                            } else {
                                processEvents(watchResponse);
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            resume();
                        }

                        @Override
                        public void onCompleted() {

                        }
                    };
                    this.requestStream = this.watchStub.watch(watchResponseStreamObserver);
                }
            }
        }
        return this.requestStream;
    }


    /**
     * Process create response from etcd server
     * <p>If there is no pendingWatcher, ignore.
     * <p>If cancel flag is true or CompactRevision not equal zero means the start revision
     * has been compacted out of the store, call onCreateFailed.
     * <p>If watchID = -1, create failed, call onCreateFailed.
     * <p>If everything is Ok, create watcher, complete ListenableFuture task and put the new watcher
     * to the watchers map.
     *
     * @param response
     */
    private void processCreate(WatchResponse response) {
        Pair<Watcher.Builder, ListenableSetFuture<Watcher>> requestPair = pendingWatchers.poll();
        Watcher.Builder builder = requestPair.getKey();
        if (response.getCreated()) {
            if (response.getCanceled() || response.getCompactRevision() != 0) {
                builder.withCanceled(true);
                Watcher watcher = builder.build();
                requestPair.getValue().setResult(watcher);
            }

            builder.withWatchID(response.getWatchId());
            Watcher watcher = builder.build();
            requestPair.getValue().setResult(watcher);

            if (response.getWatchId() == -1 && watcher.callback != null) {
                watcher.callback.onCreateFailed(response);
            } else {
                this.watchers.put(watcher.getWatchID(), watcher);
            }

            //note the header revision so that put following a current watcher disconnect will arrive
            //on watcher channel after reconnect
            synchronized (watcher) {
                watcher.setLastRevision(response.getHeader().getRevision());
                if(watcher.isResuming()){
                    watcher.setResuming(false);
                }
            }
        }
    }

    /**
     * Process subscribe watch events
     * <p>If the watch id is not in the watchers map, scan it in the cancelWatchers map
     * if exist, ignore, otherwise cancel it.
     * <p>If the watcher exist, call the onWatch and set the last revision for resume
     *
     * @param watchResponse
     */
    private void processEvents(WatchResponse watchResponse) {
        Watcher watcher = watchers.get(watchResponse.getWatchId());
        if (watcher != null) {
            synchronized (watcher) {
                if (watchResponse.getEventsCount() != 0) {
                    List<Event> events = watchResponse.getEventsList();
                    // if on resume process, filter processed events
                    if (watcher.isResuming()) {
                        long lastRevision = watcher.getLastRevision();
                        events.removeIf((e) -> e.getKv().getModRevision() <= lastRevision);
                    }
                    watcher.setLastRevision(
                            watchResponse
                                    .getEvents(watchResponse.getEventsCount() - 1)
                                    .getKv().getModRevision());

                    if (watcher.callback != null) {
                        watcher.callback.onWatch(events);
                    }
                } else {
                    watcher.setLastRevision(watchResponse.getHeader().getRevision());
                }
            }
        } else {
            watcher = this.cancelWatchers.get(watchResponse.getWatchId());
            if (this.cancelWatchers.putIfAbsent(watcher.getWatchID(), watcher) == null) {
                cancelWatch(watcher);
            }
        }
    }

    /**
     * resume all the watchers
     *
     * @param watchers
     */
    private void resumeWatchers(Watcher[] watchers) {
        for (Watcher watcher : watchers) {
            if(watcher.callback!=null){
                watcher.callback.onResuming();
            }
            watch(watcher.getKey(), getResumeWatchOptionWithWatcher(watcher), watcher.callback);
        }
    }

    /**
     * Process cancel response from etcd server,
     *
     * @param response
     */
    private void processCanceled(WatchResponse response) {
        Watcher watcher = this.cancelWatchers.remove(response.getWatchId());
        if (watcher != null && watcher.callback != null) {
            if (watcher.callback != null) {
                watcher.setCanceled(true);
                watcher.callback.onCanceled(response);
            }
        }
    }

    /**
     * convert WatcherOption to WatchRequest
     *
     * @param key
     * @param option
     * @return
     */
    private WatchRequest optionToWatchCreateRequest(ByteString key, WatchOption option) {
        WatchCreateRequest.Builder builder = WatchCreateRequest.newBuilder()
                .setKey(key)
                .setPrevKv(option.isPrevKV())
                .setProgressNotify(option.isProgressNotify())
                .setStartRevision(option.getRevision());

        if (option.getEndKey().isPresent()) {
            builder.setRangeEnd(option.getEndKey().get());
        }

        if (option.isNoDelete()) {
            builder.addFilters(WatchCreateRequest.FilterType.NODELETE);
        }

        if (option.isNoPut()) {
            builder.addFilters(WatchCreateRequest.FilterType.NOPUT);
        }

        return WatchRequest.newBuilder().setCreateRequest(builder).build();
    }

    /**
     * build new WatchOption from dead to resume it in new requestStream
     *
     * @param watcher
     * @return
     */
    private WatchOption getResumeWatchOptionWithWatcher(Watcher watcher) {
        WatchOption oldOption = watcher.getWatchOption();
        return WatchOption.newBuilder().withNoDelete(oldOption.isNoDelete())
                .withNoPut(oldOption.isNoPut())
                .withPrevKV(oldOption.isPrevKV())
                .withProgressNotify(oldOption.isProgressNotify())
                .withRange(oldOption.getEndKey().get())
                .withRevision(watcher.getLastRevision()+1)
                .withResuming(true)
                .build();
    }


}
