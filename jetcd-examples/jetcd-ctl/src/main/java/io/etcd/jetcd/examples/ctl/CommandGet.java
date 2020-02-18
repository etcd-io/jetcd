/*
 * Copyright 2016-2020 The jetcd authors
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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import org.jooq.lambda.fi.util.function.CheckedConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Charsets.UTF_8;

@Parameters(separators = "=", commandDescription = "Gets the key")
class CommandGet implements CheckedConsumer<Client> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandGet.class);

    @Parameter(arity = 1, description = "<key>")
    String key = "";

    @Parameter(names = "--rev", description = "Specify the kv revision")
    Long rev = 0L;

    @Override
    public void accept(Client client) throws Exception {
        GetResponse getResponse = client.getKVClient()
            .get(ByteSequence.from(key, UTF_8), GetOption.newBuilder().withRevision(rev).build()).get();

        if (getResponse.getKvs().isEmpty()) {
            // key does not exist
            return;
        }

        LOGGER.info(key);
        LOGGER.info(getResponse.getKvs().get(0).getValue().toString(UTF_8));
    }
}
