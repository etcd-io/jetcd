package com.coreos.jetcd;

import com.coreos.jetcd.data.ByteSequence;

/**
 * Constants of Etcd.
 */
public class Constants {

  public static final String TOKEN = "token";
  public static final ByteSequence NULL_KEY = ByteSequence.fromBytes(new byte[]{'\0'});
}
