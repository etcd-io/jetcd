package com.coreos.jetcd;

import com.coreos.jetcd.Maintenance.Snapshot;
import com.coreos.jetcd.api.StatusResponse;
import com.coreos.jetcd.exception.AuthFailedException;
import com.coreos.jetcd.exception.ConnectException;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

/**
 * Maintenance test.
 */
public class MaintenanceTest {

  private final Client client;
  private final Maintenance maintenance;
  private final Assertion test = new Assertion();
  private volatile ByteString snapshotBlob = ByteString.copyFromUtf8("");

  public MaintenanceTest() throws AuthFailedException, ConnectException {
    this.client = ClientBuilder.newBuilder().endpoints(TestConstants.endpoints).build();
    this.maintenance = client.getMaintenanceClient();
  }


  /**
   * test status member function
   */
  @Test
  public void testStatusMember() throws ExecutionException, InterruptedException {
    StatusResponse statusResponse = maintenance.statusMember().get();
    test.assertTrue(statusResponse.getDbSize() > 0);
  }

  // TODO: find a better way to test snapshot.
  @Test
  public void Testsnapshot() throws IOException {
    // create snapshot.db file current folder.
    String dir = Paths.get("").toAbsolutePath().toString();
    File snapfile = new File(dir, "snapshot.db");

    // leverage try-with-resources
    try (Snapshot snapshot = maintenance.snapshot();
        FileOutputStream fop = new FileOutputStream(snapfile)) {
      snapshot.write(fop);
    } catch (Exception e) {
      snapfile.delete();
    }
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
  void testDefragment() throws ExecutionException, InterruptedException {
    maintenance.defragmentMember().get();
  }
}
