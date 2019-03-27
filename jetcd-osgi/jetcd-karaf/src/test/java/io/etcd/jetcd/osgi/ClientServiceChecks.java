/*
 * Copyright 2016-2019 The jetcd authors
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

package io.etcd.jetcd.osgi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.resolver.URIResolver;
import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ClientServiceChecks extends TestSupport {

  private static final Charset UTF8 = Charset.forName("UTF-8");

  @Inject
  protected BundleContext bundleContext;

  @Inject
  protected Client client;

  @Inject
  @Filter("(jetcd.resolver.type=dnssrv)")
  protected URIResolver uriResolver;

  @ProbeBuilder
  public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
    probe.setHeader(
        Constants.DYNAMICIMPORT_PACKAGE,
        "*,org.apache.felix.service.*;status=provisional");

    return probe;
  }

  @Configuration
  public Option[] config() {
    final MavenArtifactUrlReference karafUrl = maven()
        .groupId("org.apache.karaf")
        .artifactId("apache-karaf-minimal")
        .versionAsInProject()
        .type("tar.gz");

    final MavenUrlReference karafStandardRepo = maven()
        .groupId("org.apache.karaf.features")
        .artifactId("standard")
        .version(getKarafVersion())
        .classifier("features")
        .type("xml");

    return new Option[] {
        systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),
        karafDistributionConfiguration()
            .frameworkUrl(karafUrl)
            .name("Apache Karaf")
            .unpackDirectory(new File("target/exam"))
            .useDeployFolder(false),
        configureSecurity()
            .disableKarafMBeanServerBuilder(),
        configureConsole()
            .ignoreLocalConsole(),
        features(
            karafStandardRepo,
            "scr",
            "wrap"),
        features(
            getFeaturesFile().toURI().toString(),
            "etcd-io-jetcd"),
        mavenBundle()
            .groupId("org.assertj")
            .artifactId("assertj-core")
            .versionAsInProject()
            .start(),
        editConfigurationFilePut("etc/io.etcd.jetcd.cfg", "endpoints", PaxExamWrapperTest.getClientEndpoints()),
        editConfigurationFilePut("etc/io.etcd.jetcd.resolver.dnssrv.cfg", "foo", "bar"),
        keepRuntimeFolder(),
        cleanCaches(),
        logLevel(LogLevelOption.LogLevel.INFO)
    };
  }

  @Test
  public void testServiceAvailability() throws Exception {
    assertThat(bundleContext).isNotNull();
    assertThat(client).isNotNull();
    assertThat(uriResolver).isNotNull();

    try {
        // It's important that we actually use jetcd, not just load it, so:
        client.getKVClient().get(ByteSequence.from("non-existing", UTF8)).get(13, TimeUnit.SECONDS);
    } catch (Throwable t) {
        // Pax Exam's WrappedTestContainerException unfortunately only includes the message, not the cause,
        // so the real reason for failures needs to be searched for in target/exam/*/data/log/karaf.log ...
        // Just for convenience in local debugging, and to eastily understand failures on CI, we dump
        // the failure's stack trace to STDOUT; like that it's easy to see in maven-surefire-plugin,
        // or when running in the IDE, like for any other non-OSGi test failure.
        t.printStackTrace();
        throw t;
    }
    // NB: Any NoClassDefFoundError/ClassNotFoundException in the log "because the bundle wiring for io.etcd.jetcd-all is no longer valid"
    // can be safely ignored - that's just jetcd/gRPC/Netty not being cleany shut down and still running.. doesn't really matter much.
  }
}
