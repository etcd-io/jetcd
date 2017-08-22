package com.coreos.jetcd.examples.jetcdctl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.Watch.Watcher;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(separators = "=", commandDescription = "Watches events stream for a key")
class CommandWatch {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandWatch.class);

  @Parameter(arity = 1, description = "<key>")
  String key;

  @Parameter(names = "--rev", description = "Revision to start watching")
  Long rev = 0L;

  // watch executes the "watch" command.
  void watch(Client client) {
    Watcher watcher = null;
    try {
      watcher = client.getWatchClient().watch(
          ByteSequence.fromString(key),
          WatchOption.newBuilder().withRevision(rev).build()
      );

      while (true) {
        WatchResponse response = watcher.listen();
        for (WatchEvent event : response.getEvents()) {
          LOGGER.info(event.getEventType().toString());
          LOGGER.info(event.getKeyValue().getKey().toStringUtf8());
          LOGGER.info(event.getKeyValue().getValue().toStringUtf8());
        }
      }

    } catch (Exception e) {
      if (watcher != null) {
        watcher.close();
      }
      LOGGER.error("Watch Error {}", e);
    }
  }
}