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

package com.coreos.jetcd.resolver;

import com.coreos.jetcd.exception.ErrorCode;
import com.coreos.jetcd.exception.EtcdExceptionFactory;
import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import io.grpc.EquivalentAddressGroup;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public final class URIResolvers {

  private URIResolvers() {
  }

  @AutoService(URIResolver.class)
  public static final class Direct implements URIResolver {

    private static final List<String> SCHEMES = Arrays.asList("http", "https");

    private ConcurrentMap<URI, List<EquivalentAddressGroup>> cache;

    public Direct() {
      this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public int priority() {
      return Integer.MIN_VALUE;
    }

    @Override
    public boolean supports(URI uri) {
      if (!SCHEMES.contains(uri.getScheme())) {
        return false;
      }

      if (!Strings.isNullOrEmpty(uri.getPath())) {
        return false;
      }

      return true;
    }

    @Override
    public List<EquivalentAddressGroup> resolve(URI uri) {
      if (!supports(uri)) {
        // Wrap as etcd exception but set a proper cause
        throw EtcdExceptionFactory.newEtcdException(
            ErrorCode.INVALID_ARGUMENT,
            "Unsupported URI " + uri
        );
      }

      return this.cache.computeIfAbsent(
          uri,
          u -> Collections.singletonList(
              new EquivalentAddressGroup(new InetSocketAddress(
                  uri.getHost(),
                  uri.getPort() != -1 ? uri.getPort() : 2739)
              )
          )
      );
    }
  }

  @AutoService(URIResolver.class)
  public static final class DnsSrv implements URIResolver {

    private static final List<String> SCHEMES = Arrays.asList("dns+srv", "dnssrv", "srv");

    private static final String[] ATTRIBUTE_IDS;
    private static final Hashtable<String, String> ENV;

    static {
      ATTRIBUTE_IDS = new String[]{"SRV"};

      ENV = new Hashtable<>();
      ENV.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
      ENV.put("java.naming.provider.url", "dns:");
    }

    private ConcurrentMap<String, EquivalentAddressGroup> cache;

    public DnsSrv() {
      this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public int priority() {
      return Integer.MAX_VALUE;
    }

    @Override
    public boolean supports(URI uri) {
      if (!SCHEMES.contains(uri.getScheme())) {
        return false;
      }

      return true;
    }

    @Override
    public List<EquivalentAddressGroup> resolve(URI uri) {
      if (!supports(uri)) {
        // Wrap as etcd exception but set a proper cause
        throw EtcdExceptionFactory.newEtcdException(
            ErrorCode.INVALID_ARGUMENT,
            "Unsupported URI " + uri
        );
      }

      List<EquivalentAddressGroup> groups = new LinkedList<>();

      try {
        DirContext ctx = new InitialDirContext(ENV);
        Attributes attributes = ctx.getAttributes(uri.getAuthority(), ATTRIBUTE_IDS);
        NamingEnumeration<?> resolved = attributes.get("srv").getAll();

        while (resolved.hasMore()) {
          String record = (String) resolved.next();
          String[] split = record.split(" ");

          if (split.length >= 4) {
            String host = split[3].trim();
            String port = split[2].trim();

            EquivalentAddressGroup group = this.cache.computeIfAbsent(
                host + ":" + port,
                k -> new EquivalentAddressGroup(new InetSocketAddress(host, Integer.parseInt(port)))
            );

            groups.add(group);
          }
        }
      } catch (Exception e) {
        throw EtcdExceptionFactory.toEtcdException(e);
      }

      return groups;
    }
  }
}
