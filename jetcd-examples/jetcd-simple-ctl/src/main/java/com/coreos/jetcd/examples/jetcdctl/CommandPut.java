package com.coreos.jetcd.examples.jetcdctl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.data.ByteSequence;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(separators = "=", commandDescription = "Puts the given key into the store")
class CommandPut {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandPut.class);

  @Parameter(arity = 2, description = "<key> <value>")
  List<String> keyValue;

  // put executes the "put" command.
  void put(Client client) {
    try {
      client.getKVClient().put(
          ByteSequence.fromString(keyValue.get(0)),
          ByteSequence.fromString(keyValue.get(1))
      ).get();
      LOGGER.info("OK");
    } catch (Exception e) {
      LOGGER.error("Put Error {}", e);
    }
  }
}
