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

import io.etcd.jetcd.resolver.URIResolver;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
    immediate = true,
    service = URIResolver.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    configurationPid = "io.etcd.jetcd.resolver.dnssrv",
    property = {
        "jetcd.resolver.type=dnssrv"
    }
)
@Designate(ocd = ClientService.Configuration.class)
public class DnsSrvResolverService implements URIResolver {
  private URIResolver delegate;

  @Override
  public boolean supports(URI uri) {
    return delegate != null ? delegate.supports(uri) : false;
  }

  @Override
  public List<SocketAddress> resolve(URI uri) {
    return delegate != null ? delegate.resolve(uri) : Collections.emptyList();
  }

  // **********************
  // Lifecycle
  // **********************

  @Activate
  protected void activate(ClientService.Configuration config) {
    this.delegate = new DnsSrvResolverService();
  }

  @Deactivate
  protected void deactivate() {
  }

  // **********************
  // Configuration
  // **********************

  @ObjectClassDefinition
  public @interface Configuration {
  }
}
