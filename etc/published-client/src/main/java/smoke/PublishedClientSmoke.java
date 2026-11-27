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

package smoke;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;

public final class PublishedClientSmoke {
    private static final String LOOPBACK = "127.0.0.1";
    private static final long REVISION = 42;

    public static void main(String[] args) throws Exception {
        var vertx = Vertx.vertx();
        try {
            var server = await(vertx.createHttpServer().requestHandler(PublishedClientSmoke::handleRange).listen(0, LOOPBACK));
            try (Client client = Client.builder("http://" + LOOPBACK + ":" + server.actualPort()).build()) {
                var key = ByteSequence.from("published-client", StandardCharsets.UTF_8);
                var response = client.getKVClient().get(key).get(5, TimeUnit.SECONDS);
                if (response.getHeader().getRevision() != REVISION) {
                    throw new AssertionError("Unexpected revision in the loopback KV response");
                }
                System.out.println("Published client completed a loopback KV request");
            }
        } finally {
            await(vertx.close());
        }
    }

    private static void handleRange(HttpServerRequest request) {
        var response = request.response();
        if (request.version() != HttpVersion.HTTP_2 || !request.path().equals("/etcdserverpb.KV/Range")) {
            response.setStatusCode(404).end();
            return;
        }

        request.body().onSuccess(ignored -> {
            var payload = io.etcd.jetcd.api.RangeResponse.newBuilder()
                .setHeader(io.etcd.jetcd.api.ResponseHeader.newBuilder().setRevision(REVISION))
                .build().toByteArray();
            // One uncompressed gRPC message, followed by the final status in HTTP/2 trailers.
            var frame = Buffer.buffer().appendByte((byte) 0).appendInt(payload.length).appendBytes(payload);
            response.putHeader("content-type", "application/grpc").putTrailer("grpc-status", "0").end(frame);
        });
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
}
