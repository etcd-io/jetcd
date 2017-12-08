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

package com.coreos.jetcd.osgi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import org.ops4j.pax.exam.ConfigurationManager;

public class TestSupport {
  private static final String FEATURES_XML = "features.xml";

  protected File getFeaturesFile() {
    String featuresXml = System.getProperty(FEATURES_XML);
    assertThat(featuresXml).isNotNull();

    File featuresFile = new File(featuresXml);
    assertThat(featuresFile).exists();

    return featuresFile;
  }

  protected File getConfigFile(String path) {
    URL res = getClass().getResource(path);
    if (res == null) {
      throw new RuntimeException("Config resource " + path + " not found");
    }

    return new File(res.getFile());
  }

  protected String getKarafVersion() {
    return new ConfigurationManager().getProperty("pax.exam.karaf.version", "4.1.4");
  }
}
