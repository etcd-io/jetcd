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

package io.etcd.jetcd.resolver;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.grpc.EquivalentAddressGroup;

import com.google.common.base.Splitter;

@SuppressWarnings("JdkObsolete")
public class DnsSrvNameResolver extends AbstractNameResolver {
    public static final String SCHEME = "dns+srv";

    private static final String[] ATTRIBUTE_IDS;
    private static final Hashtable<String, String> ENV;

    static {
        ATTRIBUTE_IDS = new String[] { "SRV" };

        ENV = new Hashtable<>();
        ENV.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        ENV.put("java.naming.provider.url", "dns:");
    }

    public DnsSrvNameResolver(URI targetUri) {
        super(targetUri);
    }

    @Override
    protected List<EquivalentAddressGroup> computeAddressGroups() {
        List<EquivalentAddressGroup> groups = new ArrayList<>();

        for (SocketAddress address : resolveAddresses()) {
            //
            // if the authority is not explicit set on the builder
            // then it will be computed from the URI
            //
            groups.add(new EquivalentAddressGroup(
                address,
                io.grpc.Attributes.EMPTY));
        }

        return groups;
    }

    private List<SocketAddress> resolveAddresses() {
        List<SocketAddress> addresses = new ArrayList<>();

        try {
            String address = getTargetUri().getPath();
            if (address.startsWith("/")) {
                address = address.substring(1);
            }

            DirContext ctx = new InitialDirContext(ENV);
            Attributes attributes = ctx.getAttributes(address, ATTRIBUTE_IDS);
            NamingEnumeration<?> resolved = attributes.get("srv").getAll();

            while (resolved.hasMore()) {
                String record = (String) resolved.next();
                List<String> split = Splitter.on(' ').splitToList(record);

                if (split.size() >= 4) {
                    String host = split.get(3).trim();
                    String port = split.get(2).trim();

                    addresses.add(new InetSocketAddress(host, Integer.parseInt(port)));
                }
            }
        } catch (Exception e) {
            throw EtcdExceptionFactory.toEtcdException(e);
        }

        return addresses;
    }
}
