/*
 * Copyright 2016-2019 The jetcd authors
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

package io.etcd.jetcd.examples.ctl;

import static com.google.common.base.Charsets.UTF_8;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Charsets;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.jooq.lambda.fi.util.function.CheckedConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(separators = "=", commandDescription = "Watches events stream for a key")
class CommandWatch implements CheckedConsumer<Client> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommandWatch.class);

  @Parameter(arity = 1, description = "<key>")
  String key;

  @Parameter(names = "--rev", description = "Revision to start watching")
  Long rev = 0L;

  @Parameter(names = { "-m", "--max-events" }, description = "the maximum number of events to receive")
  Integer maxEvents = Integer.MAX_VALUE;

  @Override
  public void accept(Client client) throws Exception {
    CountDownLatch latch = new CountDownLatch(maxEvents);
    Watcher watcher = null;

    try {
      ByteSequence watchKey = ByteSequence.from(key, Charsets.UTF_8);
      WatchOption watchOpts = WatchOption.newBuilder().withRevision(rev).build();

      watcher = client.getWatchClient().watch(watchKey, watchOpts, response -> {
            for (WatchEvent event : response.getEvents()) {
              LOGGER.info("type={}, key={}, value={}",
                event.getEventType().toString(),
                Optional.ofNullable(event.getKeyValue().getKey())
                  .map(bs -> bs.toString(UTF_8))
                  .orElse(""),
                Optional.ofNullable(event.getKeyValue().getValue())
                  .map(bs -> bs.toString(UTF_8))
                  .orElse(""));
            }

            latch.countDown();
          }
      );

      latch.await();
    } catch (Exception e) {
      if (watcher != null) {
        watcher.close();
      }

      throw e;
    }
  }
}
