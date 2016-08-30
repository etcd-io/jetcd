package com.coreos.jetcd.watch;

import com.coreos.jetcd.api.Event;
import com.coreos.jetcd.api.WatchResponse;
import com.coreos.jetcd.options.WatchOption;
import com.google.protobuf.ByteString;

import javax.annotation.concurrent.GuardedBy;
import java.util.List;

/**
 * Watcher class hold watcher information.
 */
public class Watcher {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private WatchCallback callback;
        private WatchOption watchOption;
        private ByteString key;
        private long watchID;
        private boolean canceled = false;

        public Builder withCallBack(WatchCallback callBack) {
            this.callback = callBack;
            return this;
        }

        public Builder withWatchOption(WatchOption watchOption) {
            this.watchOption = watchOption;
            return this;
        }

        public Builder withKey(ByteString key) {
            this.key = key;
            return this;
        }

        public Builder withWatchID(long watchID) {
            this.watchID = watchID;
            return this;
        }

        public Builder withCanceled(boolean canceled) {
            this.canceled = canceled;
            return this;
        }

        public Watcher build() {
            return new Watcher(this.watchID, this.key, this.watchOption, canceled, this.callback);
        }

    }


    private final WatchOption watchOption;
    private final ByteString key;

    @GuardedBy("this")
    public final WatchCallback callback;
    private final long watchID;

    @GuardedBy("this")
    private long lastRevision = -1;
    private boolean canceled = false;

    @GuardedBy("this")
    private boolean resuming;

    private Watcher(long watchID, ByteString key, WatchOption watchOption, boolean canceled, WatchCallback callback) {
        this.key = key;
        this.watchOption = watchOption;
        this.watchID = watchID;
        this.callback = callback;
        this.canceled = canceled;
        this.resuming = watchOption.isResuming();
    }

    /**
     * set the last revision watcher received, used for resume
     *
     * @param lastRevision the last revision
     */
    public synchronized void setLastRevision(long lastRevision) {
        this.lastRevision = lastRevision;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public synchronized void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * get the watch id of the watcher
     *
     * @return
     */
    public long getWatchID() {
        return watchID;
    }

    public WatchOption getWatchOption() {
        return watchOption;
    }

    /**
     * get the last revision watcher received
     *
     * @return last revision
     */
    public synchronized long getLastRevision() {
        return lastRevision;
    }

    /**
     * get the watcher key
     *
     * @return watcher key
     */
    public ByteString getKey() {
        return key;
    }

    /**
     * whether the watcher is resuming.
     */
    public synchronized boolean isResuming() {
        return resuming;
    }

    public synchronized void setResuming(boolean resuming) {
        this.resuming = resuming;
    }

    public interface WatchCallback {

        /**
         * onWatch will be called when watcher receive any events
         *
         * @param events received events
         */
        void onWatch(List<Event> events);

        /**
         * onCreateFailed will be called when create watcher failed
         *
         * @param watchResponse watch response
         */
        void onCreateFailed(WatchResponse watchResponse);

        /**
         * onResuming will be called when the watcher is on resuming.
         */
        void onResuming();

        /**
         * onCanceled will be called when the watcher is canceled successfully.
         *
         * @param response watch response for cancel
         */
        void onCanceled(WatchResponse response);
    }
}
