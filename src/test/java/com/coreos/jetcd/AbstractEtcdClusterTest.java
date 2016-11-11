package com.coreos.jetcd;

import com.coreos.jetcd.integration.EtcdInstance;
import com.coreos.jetcd.integration.ExternalInstance;
import com.coreos.jetcd.test.DockerCommandRunner;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.Arrays;
import java.util.logging.Logger;


abstract class AbstractEtcdClusterTest extends AbstractTest
{
    private static final Logger LOGGER =
            Logger.getLogger(AbstractEtcdClusterTest.class.getName());
    private static final DockerCommandRunner DOCKER_COMMAND_RUNNER =
            new DockerCommandRunner();

    private EtcdInstance[] clusterInstances;


    @AfterClass
    public synchronized void destroyCluster() throws Exception
    {
        if (clusterInstances != null)
        {
            LOGGER.info("Destroying cluster with three (3) instances.");

            for (final EtcdInstance instance : clusterInstances)
            {
                instance.destroy();
            }
        }

        clusterInstances = null;
    }


    /**
     * Depends on *NIX because of the certificates.
     *
     * @throws Exception    If the command won't run.
     */
    @BeforeClass
    public synchronized void ensureClusterRunning() throws Exception
    {
        if ((clusterInstances == null) || (clusterInstances.length == 0))
        {
            final String[] configuredEndpoints = getConfiguredClusterEndpoints();

            if (configuredEndpoints == null)
            {
                LOGGER.info("Starting cluster with three (3) instances.");
                clusterInstances = DOCKER_COMMAND_RUNNER.run(3);
            }
            else
            {
                clusterInstances =
                        new ExternalInstance[configuredEndpoints.length];
                for (int i = 0; i < clusterInstances.length; i++)
                {
                    clusterInstances[i] =
                            new ExternalInstance(configuredEndpoints[i]);
                }
            }
        }
        else
        {
            LOGGER.info("Already running on " + Arrays
                    .toString(getClusterEndpoints())
                        + " instances.");
        }
    }

    String[] getClusterEndpoints()
    {
        final String[] endpoints = new String[clusterInstances.length];

        for (int i = 0; i < clusterInstances.length; i++)
        {
            endpoints[i] = clusterInstances[i].getEndpoint();
        }

        return endpoints;
    }
}
