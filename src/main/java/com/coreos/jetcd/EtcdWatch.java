package com.coreos.jetcd;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.EtcdHeader;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface of watch client
 */
public interface EtcdWatch {


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
    CompletableFuture<Watcher> watch(ByteSequence key, WatchOption watchOption, WatchCallback callback);

    interface Watcher{

        /**
         * get watcher id
         * @return id
         */
        long getWatchID();

        long getLastRevision();

        ByteSequence getKey();

        boolean isResuming();

        /**
         * get the watch option
         * @return watch option
         */
        WatchOption getWatchOption();

        /**
         * cancel the watcher
         * @return cancel result
         */
        CompletableFuture<Boolean> cancel();
    }

    interface WatchCallback {

        /**
         * onWatch will be called when watcher receive any events
         *
         * @param events received events
         */
        void onWatch(EtcdHeader header, List<WatchEvent> events);

        /**
         * onResuming will be called when the watcher is on resuming.
         */
        void onResuming();
    }
}
