package com.coreos.jetcd;

import com.google.protobuf.ByteString;

/**
 * Constants of Etcd
 */
public class EtcdConstants {
    public static final String TOKEN = "token";
    public static final ByteString NULL_KEY = ByteString.copyFrom(new byte[] { '\0' });
}
