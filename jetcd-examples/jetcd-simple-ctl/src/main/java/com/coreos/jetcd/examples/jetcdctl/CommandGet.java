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

  // get executes the "get" command.
  void get(Client client) throws Exception {
    GetResponse getResponse = client.getKVClient().get(
        ByteSequence.fromString(key),
        GetOption.newBuilder().withRevision(rev).build()
    ).get();
    if (getResponse.getKvs().isEmpty()) {
      // key does not exist
      return;
    }
    LOGGER.info(key);
    LOGGER.info(getResponse.getKvs().get(0).getValue().toStringUtf8());
  }
}
