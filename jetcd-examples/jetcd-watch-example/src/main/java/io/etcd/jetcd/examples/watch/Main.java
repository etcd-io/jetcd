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

package io.etcd.jetcd.examples.watch;

import static com.google.common.base.Charsets.UTF_8;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    Args cmd = new Args();

    JCommander.newBuilder()
        .addObject(cmd)
        .build()
        .parse(args);

    try (Client client = Client.builder().endpoints(cmd.endpoints.toArray(new String[0])).build();
         Watch watch = client.getWatchClient();
         Watch.Watcher watcher = watch.watch(ByteSequence.from(cmd.key, UTF_8))) {
      for (int i = 0; i < cmd.maxEvents; i++) {
        LOGGER.info("Watching for key={}", cmd.key);
        WatchResponse response = watcher.listen();

        for (WatchEvent event : response.getEvents()) {
          LOGGER.info("type={}, key={}, value={}",
              event.getEventType(),
              Optional.ofNullable(event.getKeyValue().getKey())
                  .map(bs -> bs.toString(UTF_8))
                  .orElse(""),
              Optional.ofNullable(event.getKeyValue().getValue())
                  .map(bs -> bs.toString(UTF_8))
                  .orElse("")
          );
        }
      }
    } catch (Exception e) {
      LOGGER.error("Watching Error {}", e);
      System.exit(1);
    }
  }

  public static class Args {
    @Parameter(
        required = true,
        names = { "-e", "--endpoints" },
        description = "the etcd endpoints"
    )
    private final List<String> endpoints = new ArrayList<>();

    @Parameter(
        required = true,
        names = { "-k", "--key" },
        description = "the key to watch"
    )
    private String key;

    @Parameter(
        names = { "-m", "--max-events" },
        description = "the maximum number of events to receive"
    )
    private final Integer maxEvents = Integer.MAX_VALUE;
  }
}
