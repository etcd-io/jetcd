package com.coreos.jetcd;


import com.coreos.jetcd.integration.EtcdInstance;
import com.coreos.jetcd.integration.ExternalInstance;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.logging.Logger;


/**
 * Test to provide running instances for tests, if necessary.
 *
 * Set the ENDPOINTS System property if you have a running instance already.
 */
abstract class AbstractEtcDInstanceTest extends AbstractTest
{
    private static final Logger LOGGER =
            Logger.getLogger(AbstractEtcDInstanceTest.class.getName());
    private static final DockerCommandRunner DOCKER_COMMAND_RUNNER =
            new DockerCommandRunner();


    EtcdInstance etcdInstance;


    @AfterClass
    public void destroy() throws Exception
    {
        if (etcdInstance != null)
        {
            LOGGER.info("\nDestroying instance " + etcdInstance.getEndpoint()
                        + "\n");
            etcdInstance.destroy();
        }
    }

    /**
     * Spin up a Docker etcd instance if no endpoints are specified.
     *
     * @throws Exception    For any problems starting up.
     */
    @BeforeClass
    public void ensureRunningInstance() throws Exception
    {
        LOGGER.info("\n" + System.currentTimeMillis() + "\n");
        if (etcdInstance == null)
        {
            final String endpoint = getConfiguredSingleEndpoint();

            etcdInstance = (endpoint == null)
                                ? DOCKER_COMMAND_RUNNER.run()
                                : new ExternalInstance(endpoint);

            LOGGER.info("\nCreated instance " + etcdInstance.getEndpoint()
                        + "\n");
        }
        else
        {
            LOGGER.info("\nInstance " + etcdInstance.getEndpoint()
                        + " already running.\n");
        }
    }
}
