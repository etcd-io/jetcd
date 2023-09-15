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

import picocli.CommandLine;

@CommandLine.Command(name = "jetcdctl", version = "1.0", mixinStandardHelpOptions = true, subcommands = {
        CommandWatch.class,
        CommandGet.class,
        CommandPut.class
})
public class Main implements Runnable {
    @CommandLine.Option(names = { "--endpoints" }, description = "gRPC endpoints", defaultValue = "http://127.0.0.1:2379")
    String endpoints;

    @CommandLine.Option(names = { "--waitForReady" }, description = "waitForReady", defaultValue = "true")
    boolean waitForReady;

    @CommandLine.Option(names = { "--connect-timeout" }, description = "connectTimeout", defaultValue = "120s")
    String connectTimeout;

    @Override
    public void run() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
