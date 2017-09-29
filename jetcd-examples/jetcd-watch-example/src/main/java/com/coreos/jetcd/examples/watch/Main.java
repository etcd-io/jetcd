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

package com.coreos.jetcd.examples.watch;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchResponse;
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

    try (Client client = Client.builder().endpoints(cmd.endpoints).build();
         Watch watch = client.getWatchClient();
         Watch.Watcher watcher = watch.watch(ByteSequence.fromString(cmd.key))) {
      for (int i = 0; i < cmd.maxEvents; i++) {
        LOGGER.info("Watching for key={}", cmd.key);
        WatchResponse response = watcher.listen();

        for (WatchEvent event : response.getEvents()) {
          LOGGER.info("type={}, key={}, value={}",
              event.getEventType(),
              Optional.ofNullable(event.getKeyValue().getKey())
                  .map(ByteSequence::toStringUtf8)
                  .orElse(""),
              Optional.ofNullable(event.getKeyValue().getValue())
                  .map(ByteSequence::toStringUtf8)
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
    private List<String> endpoints = new ArrayList<>();

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
    private Integer maxEvents = Integer.MAX_VALUE;
  }
}
