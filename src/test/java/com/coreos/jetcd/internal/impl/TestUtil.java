package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.data.ByteSequence;
import java.io.IOException;
import java.net.ServerSocket;

public class TestUtil {

  public static String randomString() {
    return java.util.UUID.randomUUID().toString();
  }

  public static ByteSequence randomByteSequence() {
    return ByteSequence.fromString(randomString());
  }

  public static int findNextAvailablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
