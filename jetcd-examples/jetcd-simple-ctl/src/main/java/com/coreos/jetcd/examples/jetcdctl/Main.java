package com.coreos.jetcd.examples.jetcdctl;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.coreos.jetcd.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
  // global options
  @Parameter(names = {"--endpoints"}, description = "gRPC endpoints ")
  private String endpoints = "http://127.0.0.1:2379";

  @Parameter(names = {"-h", "--help"}, help = true)
  private boolean help = false;

  public static void main(String[] args) {
    Main main = new Main();
    CommandGet getCmd = new CommandGet();
    CommandPut putCmd = new CommandPut();
    CommandWatch watchCmd = new CommandWatch();
    JCommander jc = JCommander.newBuilder()
        .addObject(main)
        .addCommand("get", getCmd)
        .addCommand("put", putCmd)
        .addCommand("watch", watchCmd)
        .build();

    jc.parse(args);

    if (main.help) {
      jc.usage();
      return;
    }

    Client client = Client.builder()
        .endpoints(main.endpoints.split(","))
        .build();

    if (jc.getParsedCommand() == null) {
      return;
    }

    switch (jc.getParsedCommand()) {
      case "get":
        getCmd.get(client);
        break;
      case "put":
        putCmd.put(client);
        break;
      case "watch":
        watchCmd.watch(client);
        break;
    }
    client.close();
  }
}