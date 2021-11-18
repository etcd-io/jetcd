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
import java.net.URI;
import java.util.Collections;
import java.util.List;

import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;

import com.google.common.base.Strings;

public class HttpsNameResolver extends AbstractNameResolver {
    public static final String SCHEME = "https";

    private final URI address;

    public HttpsNameResolver(URI targetUri) {
        super(targetUri);

        this.address = targetUri;
    }

    @Override
    protected List<EquivalentAddressGroup> computeAddressGroups() {
        if (address == null) {
            throw EtcdExceptionFactory.newEtcdException(
                ErrorCode.INVALID_ARGUMENT,
                "Unable to resolve endpoint " + getTargetUri());
        }

        return Collections.singletonList(
            new EquivalentAddressGroup(
                new InetSocketAddress(
                    address.getHost(),
                    address.getPort() != -1 ? address.getPort() : ETCD_CLIENT_PORT),
                Strings.isNullOrEmpty(getServiceAuthority())
                    ? Attributes.newBuilder()
                        .set(EquivalentAddressGroup.ATTR_AUTHORITY_OVERRIDE, address.toString())
                        .build()
                    : Attributes.EMPTY));
    }
}
