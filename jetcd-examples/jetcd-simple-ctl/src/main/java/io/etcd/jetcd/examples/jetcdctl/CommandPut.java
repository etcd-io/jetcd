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

package io.etcd.jetcd.examples.jetcdctl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.data.ByteSequence;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(separators = "=", commandDescription = "Puts the given key into the store")
class CommandPut {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandPut.class);

  @Parameter(arity = 2, description = "<key> <value>")
  List<String> keyValue;

  // put executes the "put" command.
  void put(Client client) throws Exception {
    client.getKVClient().put(
        ByteSequence.fromString(keyValue.get(0)),
        ByteSequence.fromString(keyValue.get(1))
    ).get();
    LOGGER.info("OK");
  }
}
