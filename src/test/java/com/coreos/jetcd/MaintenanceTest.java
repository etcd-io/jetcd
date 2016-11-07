package com.coreos.jetcd;

import com.coreos.jetcd.api.SnapshotResponse;
import com.coreos.jetcd.api.StatusResponse;
import com.google.protobuf.ByteString;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;


/**
 * Maintenance test.
 */
public class MaintenanceTest extends DockerSetupTest
{
    private EtcdMaintenance maintenance;
    private Assertion test = new Assertion();
    private volatile ByteString snapshotBlob;
    private CountDownLatch finishLatch = new CountDownLatch(1);

    private DockerContainer etcdInstance;


    @BeforeSuite
    public void ensureInstanceRunning() throws Exception
    {
        pullLatestImage();
        this.etcdInstance = runSingleInstance();

        final EtcdClient etcdClient = EtcdClientBuilder.newBuilder()
                .endpoints(etcdInstance.getEndpoint()).build();
        maintenance = etcdClient.getMaintenanceClient();
    }

    @AfterSuite
    public void killInstance() throws Exception
    {
        if (etcdInstance != null)
        {
            etcdInstance.destroy();
        }
    }


    /**
     * test status member function
     */
    @Test
    public void testStatusMember() throws ExecutionException,
                                          InterruptedException
    {
        StatusResponse statusResponse = maintenance.statusMember().get();
        test.assertTrue(statusResponse.getDbSize() > 0);
    }

    /**
     * test alarm list function
     * TODO trigger alarm, valid whether listAlarms will work.
     * TODO disarm the alarm member, valid whether disarm will work with listAlarms.
     */
    @Test
    public void testAlarmList() throws ExecutionException, InterruptedException
    {
        maintenance.listAlarms().get();
    }

    /**
     * test setSnapshotCallback function
     * TODO trigger snapshot, valid whether setSnapshotCallback will work.
     * TODO test removeSnapShotCallback
     */
    @Test
    void testAddSnapshotCallback()
    {
        maintenance.setSnapshotCallback(new EtcdMaintenance.SnapshotCallback()
        {
            @Override
            public synchronized void onSnapShot(SnapshotResponse snapshotResponse)
            {
                // blob contains the next chunk of the snapshot in the snapshot stream, blob is the bytes snapshot.
                // remaining_bytes is the number of blob bytes to be sent after this message
                if (snapshotBlob == null)
                {
                    snapshotBlob = snapshotResponse.getBlob();
                }
                else
                {
                    snapshotBlob = snapshotBlob
                            .concat(snapshotResponse.getBlob());
                }
                if (snapshotResponse.getRemainingBytes() == 0)
                {
                    // TODO finishLatch will be replaced by ListenableFuture instance
                    finishLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable throwable)
            {

            }
        });
    }

    /**
     * test defragmentMember function
     */
    @Test
    void testDefragment() throws ExecutionException, InterruptedException
    {
        maintenance.defragmentMember().get();
    }
}
