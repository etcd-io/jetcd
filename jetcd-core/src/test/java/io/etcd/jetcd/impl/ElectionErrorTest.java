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

package io.etcd.jetcd.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.election.LeaderKey;
import io.etcd.jetcd.election.NoLeaderException;
import io.etcd.jetcd.election.NotLeaderException;
import io.etcd.jetcd.test.EtcdConstants;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcMessageDecoder;
import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceMethod;
import io.vertx.grpc.server.GrpcServer;

import com.google.protobuf.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(30)
class ElectionErrorTest {
    private static final ByteSequence ELECTION = TestUtil.bytesOf("election");
    private static final ByteSequence PROPOSAL = TestUtil.bytesOf("proposal");
    private static final LeaderKey LEADER = new LeaderKey(ELECTION, TestUtil.bytesOf("key"), 1, 1);

    private final Vertx vertx = Vertx.vertx();

    @AfterEach
    void closeVertx() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    @Test
    void mapsExplicitNoLeaderError() throws Exception {
        try (Client client = startServer(GrpcStatus.UNKNOWN, "election: no leader")) {
            var response = client.getElectionClient().leader(ELECTION);
            assertThatThrownBy(() -> response.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(NoLeaderException.class);
        }
    }

    @Test
    void mapsExplicitNotLeaderError() throws Exception {
        try (Client client = startServer(GrpcStatus.UNKNOWN, "election: not leader")) {
            var response = client.getElectionClient().proclaim(LEADER, PROPOSAL);
            assertThatThrownBy(() -> response.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(NotLeaderException.class);
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
            "UNKNOWN, storage error mentioning election: no leader, UNKNOWN",
            "UNAVAILABLE, election: no leader, UNAVAILABLE",
            "PERMISSION_DENIED, access denied, PERMISSION_DENIED",
            "UNKNOWN, NULL, UNKNOWN"
    }, nullValues = "NULL")
    void preservesUnrelatedFailures(GrpcStatus status, String message, ErrorCode code) throws Exception {
        try (Client client = startServer(status, message)) {
            var election = client.getElectionClient();
            assertFailure(election.leader(ELECTION), status, message, code);
            assertFailure(election.campaign(ELECTION, 1, PROPOSAL), status, message, code);
            assertFailure(election.proclaim(LEADER, PROPOSAL), status, message, code);
            assertFailure(election.resign(LEADER), status, message, code);
        }
    }

    private Client startServer(GrpcStatus status, String message) throws Exception {
        var grpc = GrpcServer.server(vertx);
        failWithStatus(grpc, io.etcd.jetcd.api.ElectionGrpcClient.Leader, io.etcd.jetcd.api.LeaderRequest.getDefaultInstance(),
            status, message);
        failWithStatus(grpc, io.etcd.jetcd.api.ElectionGrpcClient.Campaign,
            io.etcd.jetcd.api.CampaignRequest.getDefaultInstance(),
            status, message);
        failWithStatus(grpc, io.etcd.jetcd.api.ElectionGrpcClient.Proclaim,
            io.etcd.jetcd.api.ProclaimRequest.getDefaultInstance(),
            status, message);
        failWithStatus(grpc, io.etcd.jetcd.api.ElectionGrpcClient.Resign, io.etcd.jetcd.api.ResignRequest.getDefaultInstance(),
            status, message);

        var server = vertx.createHttpServer().requestHandler(grpc).listen(0, EtcdConstants.LOCALHOST)
            .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        return Client.builder("http://" + EtcdConstants.LOCALHOST + ":" + server.actualPort()).vertx(vertx).build();
    }

    private static <Req extends Message, Resp extends Message> void failWithStatus(GrpcServer server,
        ServiceMethod<Resp, Req> clientMethod, Req prototype, GrpcStatus status, String message) {
        var method = ServiceMethod.<Req, Resp> server(clientMethod.serviceName(), clientMethod.methodName(),
            clientMethod.cardinality(), GrpcMessageEncoder.encoder(), GrpcMessageDecoder.decoder(prototype));
        server.callHandler(method, request -> request.handler(ignored -> {
            var response = request.response().status(status);
            if (message != null) {
                response.statusMessage(message);
            }
            response.end();
        }));
    }

    private static void assertFailure(CompletableFuture<?> future, GrpcStatus status, String message, ErrorCode code) {
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).cause().isInstanceOfSatisfying(EtcdException.class, error -> {
            assertThat(error.getErrorCode()).isEqualTo(code);
            assertThat(error.getMessage()).isEqualTo(message == null ? status.toString() : message);
            assertThat(error.getCause()).isInstanceOfSatisfying(InvalidStatusException.class,
                original -> assertThat(original.actualStatus()).isEqualTo(status));
        });
    }
}
