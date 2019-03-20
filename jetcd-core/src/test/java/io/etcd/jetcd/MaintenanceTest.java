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
package io.etcd.jetcd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import io.etcd.jetcd.launcher.junit.EtcdClusterResource;
import io.etcd.jetcd.maintenance.SnapshotResponse;
import io.etcd.jetcd.maintenance.StatusResponse;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Maintenance test.
 */
public class MaintenanceTest {
  @ClassRule
  public static EtcdClusterResource clusterResource = new EtcdClusterResource("etcd-maintenance", 3 ,false);

  private Client client;
  private Maintenance maintenance;
  private List<URI> endpoints;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    this.endpoints = clusterResource.cluster().getClientEndpoints();
    this.client = Client.builder().endpoints(endpoints).build();
    this.maintenance = client.getMaintenanceClient();
  }

  @After
  public void tearDown() {
  }

  /**
   * test status member function
   */
  @Test
  public void testStatusMember() throws ExecutionException, InterruptedException {
    StatusResponse statusResponse = maintenance.statusMember(endpoints.get(0)).get();
    assertThat(statusResponse.getDbSize()).isGreaterThan(0);
  }

  @Test
  public void testSnapshotToOutputStream() throws ExecutionException, InterruptedException, IOException {
    // create a snapshot file current folder.
    final Path snapfile = temporaryFolder.newFile().toPath();

    // leverage try-with-resources
    try (OutputStream stream = Files.newOutputStream(snapfile)) {
      Long bytes = maintenance.snapshot(stream).get();

      stream.flush();

      Long fsize = Files.size(snapfile);

      assertThat(bytes).isEqualTo(fsize);
    }
  }

  @Test
  public void testSnapshotChunks() throws ExecutionException, InterruptedException {
    final Long bytes = maintenance.snapshot(NullOutputStream.NULL_OUTPUT_STREAM).get();
    final AtomicLong count = new AtomicLong();
    final CountDownLatch latcht = new CountDownLatch(1);

    maintenance.snapshot(new StreamObserver<SnapshotResponse>() {
      @Override
      public void onNext(SnapshotResponse value) {
        count.addAndGet(value.getBlob().size());
      }

      @Override
      public void onError(Throwable t) {
        fail("Should not throw exception");
      }

      @Override
      public void onCompleted() {
        latcht.countDown();
      }
    });

    latcht.await(10, TimeUnit.SECONDS);

    assertThat(bytes).isEqualTo(count.get());
  }

  @Test
  public void testHashKV() throws ExecutionException, InterruptedException {
    maintenance.hashKV(endpoints.get(0), 0).get();
  }

  /**
   * test alarm list function
   * TODO trigger alarm, valid whether listAlarms will work.
   * TODO disarm the alarm member, valid whether disarm will work with listAlarms.
   */
  @Test
  public void testAlarmList() throws ExecutionException, InterruptedException {
    maintenance.listAlarms().get();
  }

  /**
   * test defragmentMember function
   */
  @Test
  public void testDefragment() throws ExecutionException, InterruptedException {
    maintenance.defragmentMember(endpoints.get(0)).get();
  }

  @Test
  public void testMoveLeader() throws ExecutionException, InterruptedException {
    URI leaderEndpoint = null;
    List<Long> followers = new ArrayList<>();
    for(URI ep : endpoints){
      StatusResponse statusResponse = maintenance.statusMember(ep).get();
      long memberId = statusResponse.getHeader().getMemberId();
      if (memberId == statusResponse.getLeader()) {
        leaderEndpoint = ep;
        continue;
      }
      followers.add(memberId);
    }
    if (leaderEndpoint == null) {
      fail("leader not found");
    }

    try(Client client = Client.builder().endpoints(leaderEndpoint).build()) {
      client.getMaintenanceClient().moveLeader(followers.get(0)).get();
    }
  }
}
