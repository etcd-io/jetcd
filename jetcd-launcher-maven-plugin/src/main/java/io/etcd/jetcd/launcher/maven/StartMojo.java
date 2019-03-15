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

package io.etcd.jetcd.launcher.maven;

import static com.google.common.base.Charsets.US_ASCII;

import com.google.common.io.Files;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts etcd launcher.
 *
 * @author Michael Vorburger.ch
 */
@Mojo(name = "start", requiresProject = false,
      defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartMojo extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(StartMojo.class);

  @Parameter(required = true, defaultValue = "target/jetcd-launcher-maven-plugin/endpoint")
  private File endpointFile;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Singleton.etcd = EtcdClusterFactory.buildCluster("maven", 1, false);
    Singleton.etcd.start();

    URI endpoint = Singleton.etcd.getClientEndpoints().get(0);
    try {
      endpointFile.getParentFile().mkdirs();
      Files.asCharSink(endpointFile, US_ASCII).write(endpoint.toString());
      LOG.info("{} = {}", endpointFile, endpoint);
    } catch (IOException e) {
      throw new MojoFailureException("writing file failed", e);
    }
  }
}
