package com.coreos.jetcd;

import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.util.ListenableSetFuture;
import com.coreos.jetcd.watch.Watcher;
import com.google.protobuf.ByteString;

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
    ListenableSetFuture<Watcher> watch(ByteString key, WatchOption watchOption, Watcher.WatchCallback callback);

    /**
     * Cancel the watch task with the watcher, the onCanceled will be called after successfully canceled.
     *
     * @param watcher the watcher to be canceled
     */
    void cancelWatch(Watcher watcher);
}
