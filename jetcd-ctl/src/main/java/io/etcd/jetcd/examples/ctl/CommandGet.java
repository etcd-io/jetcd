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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;

import picocli.CommandLine;

import static com.google.common.base.Charsets.UTF_8;

@CommandLine.Command(name = "get", description = "Gets the key")
class CommandGet implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandGet.class);

    @CommandLine.ParentCommand
    Main main;

    @CommandLine.Parameters(index = "0", arity = "1", description = "key")
    String key;

    @CommandLine.Option(names = "--rev", description = "Revision to start watching", defaultValue = "0")
    Long rev;

    @Override
    public void run() {
        try (Client client = Client.builder().endpoints(main.endpoints).build()) {
            GetResponse getResponse = client.getKVClient().get(
                ByteSequence.from(key, UTF_8),
                GetOption.newBuilder().withRevision(rev).build()).get();

            if (getResponse.getKvs().isEmpty()) {
                // key does not exist
                return;
            }

            LOGGER.info(key);
            LOGGER.info(getResponse.getKvs().get(0).getValue().toString(UTF_8));
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
