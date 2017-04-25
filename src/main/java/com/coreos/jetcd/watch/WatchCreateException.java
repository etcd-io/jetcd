package com.coreos.jetcd.watch;

import com.coreos.jetcd.data.EtcdHeader;

/**
 * Exception thrown when create watcher failed.
 */
public class WatchCreateException extends Exception {

    public final EtcdHeader header;

    public WatchCreateException(String cause, EtcdHeader header) {
        super(cause);
        this.header = header;
    }
}
