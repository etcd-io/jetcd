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

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;

import picocli.CommandLine;

@CommandLine.Command(name = "put", description = "Puts the given key into the store")
class CommandPut implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandPut.class);

    @CommandLine.ParentCommand
    Main main;

    @CommandLine.Parameters(index = "0", arity = "1", description = "key")
    String key;
    @CommandLine.Parameters(index = "1", arity = "1", description = "value")
    String val;

    @Override
    public void run() {
        try (Client client = Client.builder().endpoints(main.endpoints).build()) {
            client.getKVClient().put(
                ByteSequence.from(key, StandardCharsets.UTF_8),
                ByteSequence.from(val, StandardCharsets.UTF_8)).get();

            LOGGER.info("OK");
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
