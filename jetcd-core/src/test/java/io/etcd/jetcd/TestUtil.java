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

import com.google.common.base.Charsets;
import java.io.IOException;
import java.net.ServerSocket;

public class TestUtil {

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
}
