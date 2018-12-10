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

package io.etcd.jetcd.examples.ctl;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.etcd.jetcd.Client;
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

    String parsedCmd = jc.getParsedCommand();
    if (parsedCmd == null || main.help) {
      jc.usage();
      return;
    }

    try (Client client = Client.builder()
            .endpoints(main.endpoints.split(","))
            .build()) {
      switch (parsedCmd) {
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
    } catch (Exception e) {
      LOGGER.error(parsedCmd + " Error {}", e);
      System.exit(1);
    }
  }
}