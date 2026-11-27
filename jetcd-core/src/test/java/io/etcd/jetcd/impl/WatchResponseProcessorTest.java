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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.etcd.jetcd.api.Event;
import io.etcd.jetcd.api.KeyValue;
import io.etcd.jetcd.api.ResponseHeader;
import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.common.exception.CompactedException;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.Errors;

import com.google.protobuf.ByteString;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WatchResponseProcessor state handling.
 */
@Tag("watch")
class WatchResponseProcessorTest {

    private WatchResponseProcessor processorWithNotifications;
    private WatchResponseProcessor processorWithoutNotifications;

    @BeforeEach
    void setUp() {
        WatchOption optionWithNotifications = WatchOption.builder()
            .withCreateNotify(true)
            .withProgressNotify(true)
            .build();
        processorWithNotifications = new WatchResponseProcessor(optionWithNotifications);

        WatchOption optionWithoutNotifications = WatchOption.builder()
            .withCreateNotify(false)
            .withProgressNotify(false)
            .build();
        processorWithoutNotifications = new WatchResponseProcessor(optionWithoutNotifications);
    }

    // Helper methods for creating test responses

    private ResponseHeader createHeader(long revision) {
        return ResponseHeader.newBuilder()
            .setRevision(revision)
            .setClusterId(1)
            .setMemberId(1)
            .build();
    }

    private WatchResponse createCreatedResponse(long watchId, long revision) {
        return WatchResponse.newBuilder()
            .setCreated(true)
            .setWatchId(watchId)
            .setHeader(createHeader(revision))
            .build();
    }

    private WatchResponse createCanceledResponse(String reason, long compactRevision) {
        WatchResponse.Builder builder = WatchResponse.newBuilder()
            .setCanceled(true)
            .setHeader(createHeader(100));

        if (reason != null && !reason.isEmpty()) {
            builder.setCancelReason(reason);
        }
        if (compactRevision > 0) {
            builder.setCompactRevision(compactRevision);
        }

        return builder.build();
    }

    private WatchResponse createAuthErrorResponse(String authErrorMessage) {
        return WatchResponse.newBuilder()
            .setCreated(true)
            .setCanceled(true)
            .setCancelReason(authErrorMessage)
            .setHeader(createHeader(100))
            .build();
    }

    private WatchResponse createProgressResponse(long revision) {
        return WatchResponse.newBuilder()
            .setHeader(createHeader(revision))
            .build();
    }

    private WatchResponse createEventResponse(int eventCount, long lastRevision) {
        WatchResponse.Builder builder = WatchResponse.newBuilder()
            .setHeader(createHeader(100));

        for (int i = 0; i < eventCount; i++) {
            long revision = lastRevision - (eventCount - 1 - i);
            builder.addEvents(createPutEvent("key" + i, "value" + i, revision));
        }

        return builder.build();
    }

    private Event createPutEvent(String key, String value, long revision) {
        return Event.newBuilder()
            .setType(Event.EventType.PUT)
            .setKv(KeyValue.newBuilder()
                .setKey(ByteString.copyFromUtf8(key))
                .setValue(ByteString.copyFromUtf8(value))
                .setModRevision(revision)
                .build())
            .build();
    }

    // Test cases for Watch Created responses

    @Test
    void testProcessWatchCreatedWithValidId() {
        WatchResponse response = createCreatedResponse(1, 100);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Created.class);
        WatchResponseProcessor.Result.Created created = (WatchResponseProcessor.Result.Created) result;
        assertThat(created.revision()).isEqualTo(100L);
        assertThat(created.shouldNotify()).isTrue();
    }

    @Test
    void testProcessWatchCreatedWithInvalidId() {
        WatchResponse response = createCreatedResponse(-1, 100);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Canceled.class);
        WatchResponseProcessor.Result.Canceled canceled = (WatchResponseProcessor.Result.Canceled) result;
        assertThat(canceled.error()).isInstanceOf(EtcdException.class);
        assertThat(canceled.error()).hasMessageContaining("failed to create watch id");
    }

    @Test
    void testProcessWatchCreatedWithNotifyEnabled() {
        WatchResponse response = createCreatedResponse(1, 100);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Created.class);
        WatchResponseProcessor.Result.Created created = (WatchResponseProcessor.Result.Created) result;
        assertThat(created.shouldNotify()).isTrue();
    }

    @Test
    void testProcessWatchCreatedWithNotifyDisabled() {
        WatchResponse response = createCreatedResponse(1, 100);

        WatchResponseProcessor.Result result = processorWithoutNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Created.class);
        WatchResponseProcessor.Result.Created created = (WatchResponseProcessor.Result.Created) result;
        assertThat(created.shouldNotify()).isFalse();
    }

    @Test
    void testProcessWatchCreatedWithRevision() {
        WatchResponse response = createCreatedResponse(1, 12345);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Created.class);
        WatchResponseProcessor.Result.Created created = (WatchResponseProcessor.Result.Created) result;
        assertThat(created.revision()).isEqualTo(12345L);
    }

    // Test cases for Watch Canceled responses

    @Test
    void testProcessWatchCanceledWithCompactRevision() {
        WatchResponse response = createCanceledResponse(null, 50);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Canceled.class);
        WatchResponseProcessor.Result.Canceled canceled = (WatchResponseProcessor.Result.Canceled) result;
        assertThat(canceled.error()).isInstanceOf(CompactedException.class);
        CompactedException compactedException = (CompactedException) canceled.error();
        assertThat(compactedException.getCompactedRevision()).isEqualTo(50);
    }

    @Test
    void testProcessWatchCanceledWithReason() {
        WatchResponse response = createCanceledResponse("watch was canceled", 0);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Canceled.class);
        WatchResponseProcessor.Result.Canceled canceled = (WatchResponseProcessor.Result.Canceled) result;
        assertThat(canceled.error()).isInstanceOf(EtcdException.class);
        assertThat(canceled.error()).hasMessageContaining("watch was canceled");
    }

    @Test
    void testProcessWatchCanceledWithEmptyReason() {
        WatchResponse response = createCanceledResponse("", 0);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Canceled.class);
        WatchResponseProcessor.Result.Canceled canceled = (WatchResponseProcessor.Result.Canceled) result;
        assertThat(canceled.error()).isInstanceOf(EtcdException.class);
        assertThat(canceled.error()).hasMessageContaining("future revision");
    }

    @Test
    void testProcessWatchCanceledWithNullReason() {
        WatchResponse response = createCanceledResponse(null, 0);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Canceled.class);
        WatchResponseProcessor.Result.Canceled canceled = (WatchResponseProcessor.Result.Canceled) result;
        assertThat(canceled.error()).isInstanceOf(EtcdException.class);
        assertThat(canceled.error()).hasMessageContaining("future revision");
    }

    // Test cases for Authentication Errors

    @Test
    void testProcessAuthErrorWithPermissionDenied() {
        WatchResponse response = createAuthErrorResponse(Errors.PERMISSION_DENIED_ERROR_MESSAGE);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.AuthError.class);
    }

    @Test
    void testProcessAuthErrorWithInvalidToken() {
        WatchResponse response = createAuthErrorResponse(Errors.INVALID_AUTH_TOKEN_ERROR_MESSAGE);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.AuthError.class);
    }

    @Test
    void testProcessNonAuthCancellation() {
        WatchResponse response = createCanceledResponse("some other reason", 0);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Canceled.class);
        assertThat(result).isNotInstanceOf(WatchResponseProcessor.Result.AuthError.class);
    }

    @Test
    void testProcessAuthErrorRequiresBothCreatedAndCanceled() {
        // Only created, not canceled
        WatchResponse response = WatchResponse.newBuilder()
            .setCreated(true)
            .setCanceled(false)
            .setCancelReason(Errors.PERMISSION_DENIED_ERROR_MESSAGE)
            .setHeader(createHeader(100))
            .setWatchId(1)
            .build();

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isNotInstanceOf(WatchResponseProcessor.Result.AuthError.class);
        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Created.class);
    }

    // Test cases for Progress Notifications

    @Test
    void testProcessProgressNotification() {
        WatchResponse response = createProgressResponse(200);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Progress.class);
        WatchResponseProcessor.Result.Progress progress = (WatchResponseProcessor.Result.Progress) result;
        assertThat(progress.revision()).isEqualTo(200L);
    }

    @Test
    void testProcessEmptyEventsWithProgressNotifyEnabled() {
        // Empty response without created/canceled flags should be treated as progress with namespace
        WatchResponse response = WatchResponse.newBuilder()
            .setHeader(createHeader(150))
            .setCreated(false)
            .setCanceled(false)
            .setCompactRevision(0)
            .build();

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Progress.class);
        WatchResponseProcessor.Result.Progress progress = (WatchResponseProcessor.Result.Progress) result;
        assertThat(progress.revision()).isEqualTo(150L);
        // This is a progress notification from the server (isProgressNotify), not namespace-based
        assertThat(progress.withNamespace()).isFalse();
    }

    @Test
    void testProcessEmptyEventsWithProgressNotifyDisabled() {
        // Even with progressNotify disabled, server-initiated progress notifications are still processed
        WatchResponse response = WatchResponse.newBuilder()
            .setHeader(createHeader(150))
            .setCreated(false)
            .setCanceled(false)
            .setCompactRevision(0)
            .build();

        WatchResponseProcessor.Result result = processorWithoutNotifications.process(response);

        // Server progress notifications are always processed regardless of progressNotify setting
        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Progress.class);
    }

    // Test cases for Event Responses

    @Test
    void testProcessEventsWithSingleEvent() {
        WatchResponse response = createEventResponse(1, 100);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Events.class);
        WatchResponseProcessor.Result.Events events = (WatchResponseProcessor.Result.Events) result;
        assertThat(events.newRevision()).isEqualTo(101L);
    }

    @Test
    void testProcessEventsWithMultipleEvents() {
        WatchResponse response = createEventResponse(3, 105);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Events.class);
        WatchResponseProcessor.Result.Events events = (WatchResponseProcessor.Result.Events) result;
        assertThat(events.newRevision()).isEqualTo(106L);
    }

    @Test
    void testProcessEventsRevisionCalculation() {
        WatchResponse response = createEventResponse(5, 200);

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Events.class);
        WatchResponseProcessor.Result.Events events = (WatchResponseProcessor.Result.Events) result;
        assertThat(events.newRevision()).isEqualTo(201L);
    }

    @Test
    void testProcessEmptyEventsReturnsProgress() {
        WatchResponse response = WatchResponse.newBuilder()
            .setHeader(createHeader(100))
            .build();

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isNotInstanceOf(WatchResponseProcessor.Result.Events.class);
        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Progress.class);
    }

    // Test cases for Ignored Responses

    @Test
    void testProcessUnrecognizedResponse() {
        // A response with revision 0 is truly unrecognized
        WatchResponse response = WatchResponse.newBuilder()
            .setHeader(ResponseHeader.newBuilder()
                .setRevision(0)
                .setClusterId(1)
                .setMemberId(1)
                .build())
            .setCreated(false)
            .setCanceled(false)
            .setCompactRevision(0)
            .build();

        WatchResponseProcessor.Result result = processorWithoutNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Ignored.class);
    }

    @Test
    void testProcessResponseWithNoSpecialFlags() {
        // Response with no events, no flags, but with revision 0 is ignored
        WatchResponse response = WatchResponse.newBuilder()
            .setHeader(ResponseHeader.newBuilder()
                .setRevision(0)
                .setClusterId(1)
                .setMemberId(1)
                .build())
            .setCreated(false)
            .setCanceled(false)
            .setCompactRevision(0)
            .build();

        WatchResponseProcessor.Result result = processorWithoutNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Ignored.class);
    }

    // Edge case tests

    @Test
    void testProcessAuthErrorTakesPrecedenceOverCreated() {
        // When both created and canceled are true with auth error, auth error is detected first
        WatchResponse response = WatchResponse.newBuilder()
            .setCreated(true)
            .setCanceled(true)
            .setWatchId(1)
            .setCancelReason(Errors.PERMISSION_DENIED_ERROR_MESSAGE)
            .setHeader(createHeader(100))
            .build();

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        // Auth error check happens before created check
        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.AuthError.class);
    }

    @Test
    void testProcessCanceledWithBothReasonAndCompactRevision() {
        WatchResponse response = WatchResponse.newBuilder()
            .setCanceled(true)
            .setCancelReason("compacted")
            .setCompactRevision(50)
            .setHeader(createHeader(100))
            .build();

        WatchResponseProcessor.Result result = processorWithNotifications.process(response);

        assertThat(result).isInstanceOf(WatchResponseProcessor.Result.Canceled.class);
        WatchResponseProcessor.Result.Canceled canceled = (WatchResponseProcessor.Result.Canceled) result;
        assertThat(canceled.error()).isInstanceOf(CompactedException.class);
    }
}
