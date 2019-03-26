/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;

public class TestUtil {

  public static ByteSequence bytesOf(final String string) {
    return ByteSequence.from(string, UTF_8);
  }

  public static ByteString byteStringOf(final String string) {
    return ByteString.copyFrom(string.getBytes(UTF_8));
  }

  public static String randomString() {
    return java.util.UUID.randomUUID().toString();
  }

  public static ByteSequence randomByteSequence() {
    return ByteSequence.from(randomString(), Charsets.UTF_8);
  }

  public static int findNextAvailablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  public static void closeQuietly(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (final IOException ioe) {
      // ignore
    }
  }
}
