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

package io.etcd.jetcd;

import io.etcd.jetcd.api.ResponseHeader;

public class AbstractResponse<R> implements Response {

    private final R response;
    private final ResponseHeader responseHeader;
    private final Header header;

    public AbstractResponse(R response, ResponseHeader responseHeader) {
        this.response = response;
        this.responseHeader = responseHeader;

        this.header = new HeaderImpl();
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return response.toString();
    }

    protected final R getResponse() {
        return this.response;
    }

    protected final ResponseHeader getResponseHeader() {
        return this.responseHeader;
    }

    private class HeaderImpl implements Response.Header {

        @Override
        public long getClusterId() {
            return responseHeader.getClusterId();
        }

        @Override
        public long getMemberId() {
            return responseHeader.getMemberId();
        }

        @Override
        public long getRevision() {
            return responseHeader.getRevision();
        }

        @Override
        public long getRaftTerm() {
            return responseHeader.getRaftTerm();
        }
    }
}
