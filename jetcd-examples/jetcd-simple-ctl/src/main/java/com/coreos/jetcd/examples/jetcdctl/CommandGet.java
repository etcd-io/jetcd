package com.coreos.jetcd.examples.jetcdctl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(separators = "=", commandDescription = "Gets the key")
class CommandGet {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandGet.class);

  @Parameter(arity = 1, description = "<key>")
  private String key = "";

  @Parameter(names = "--rev", description = "Specify the kv revision")
  private Long rev = 0L;

  // do executes the "get" command.
  void get(Client client) {
    try {
      GetResponse getResponse = client.getKVClient().get(
          ByteSequence.fromString(key),
          GetOption.newBuilder().withRevision(rev).build()
      ).get();
      LOGGER.info(key);
      LOGGER.info(getResponse.getKvs().get(0).getValue().toStringUtf8());
    } catch (Exception e) {
      LOGGER.error("Put Error {}", e);
    }
  }
}
