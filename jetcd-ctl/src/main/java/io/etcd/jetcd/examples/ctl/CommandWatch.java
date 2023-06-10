/*
 * Copyright 2016-2021 The jetcd authors
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

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;

import com.google.common.base.Charsets;

import picocli.CommandLine;

@CommandLine.Command(name = "watch", description = "Watches events stream for a key")
class CommandWatch implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandWatch.class);

    @CommandLine.ParentCommand
    Main main;

    @CommandLine.Parameters(arity = "1", description = "<key>")
    String key;

    @CommandLine.Option(names = "--rev", description = "Revision to start watching", defaultValue = "0")
    Long rev;
    @CommandLine.Option(names = { "-m", "--max-events" }, description = "the maximum number of events to receive")
    Integer maxEvents;

    @Override
    public void run() {
        CountDownLatch latch = new CountDownLatch(maxEvents != null ? maxEvents : Integer.MAX_VALUE);

        try (Client client = Client.builder().endpoints(main.endpoints).build()) {
            ByteSequence watchKey = ByteSequence.from(key, Charsets.UTF_8);
            WatchOption watchOpts = WatchOption.newBuilder().withRevision(rev != null ? rev : 0).build();

            Consumer<WatchResponse> consumer = response -> {
                for (WatchEvent event : response.getEvents()) {
                    LOGGER.info("type={}, key={}, value={}",
                        event.getEventType().toString(),
                        Optional.ofNullable(event.getKeyValue().getKey()).map(bs -> bs.toString(Charsets.UTF_8)).orElse(""),
                        Optional.ofNullable(event.getKeyValue().getValue()).map(bs -> bs.toString(Charsets.UTF_8)).orElse(""));
                }

                latch.countDown();
            };

            try (Watcher ignored = client.getWatchClient().watch(watchKey, watchOpts, consumer)) {
                // close the watcher
            }

            latch.await();

            LOGGER.info("done");
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
