package com.coreos.jetcd.test;

import com.coreos.jetcd.integration.DockerContainerInstance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Simple command runner for Docker.
 */
public class DockerCommandRunner
{
    private static final Logger LOGGER = Logger.getLogger(
            DockerCommandRunner.class.getName());
    private static final int DOCKER_PROCESS_COMPLETE = 0;
    private static final String DEFAULT_DOCKER_IMAGE_NAME =
            "quay.io/coreos/etcd";
    private static final String DOCKER_IMAGE_NAME_PROPERTY_KEY =
            "ETCD_DOCKER_IMAGE";


    public InputStream runDockerCommand(final String... command)
            throws Exception
    {
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        final Process process = processBuilder.start();

        final int exitCode = process.waitFor();

        if (exitCode == DOCKER_PROCESS_COMPLETE)
        {
            return process.getInputStream();
        }
        else
        {
            final InputStream errorStream = process.getErrorStream();

            throw new RuntimeException(
                    String.format("Unable to execute \n'%s'\n due to (%d): %s.",
                                  Arrays.toString(command),
                                  exitCode, readMessage(errorStream)));
        }
    }

    private void pullLatestImage() throws Exception
    {
        LOGGER.info("\nPulling latest image...\n");
        final String dockerImageName = getETCDDockerImageName();
        final String[] dockerPull = new String[] {
                "docker", "pull", dockerImageName
        };

        runDockerCommand(dockerPull);
    }

    private String getETCDDockerImageName()
    {
        return System.getProperty(DOCKER_IMAGE_NAME_PROPERTY_KEY,
                                  DEFAULT_DOCKER_IMAGE_NAME);
    }

    private void ensureNetworkExists(final String networkName) throws Exception
    {
        final InputStream inputStream =
                runDockerCommand("docker", "network", "ls", "-q", "--filter",
                                 "name=" + networkName);

        final String output = readMessage(inputStream);

        if (output.trim().equals(""))
        {
            LOGGER.info("Creating network " + networkName);
            runDockerCommand("docker", "network", "create", networkName);
        }
    }

    /**
     * Start a single instance.
     * @return          The running instance.
     * @throws Exception    Any errors starting it up.
     */
    public DockerContainerInstance run() throws Exception
    {
        return run(1)[0];
    }

    /**
     * Run a cluster of name:host instances.
     *
     * @param count             The number of items in the cluster.
     * @return                  Array in the cluster.
     * @throws Exception    Any errors starting it up.
     */
    public DockerContainerInstance[] run(final int count) throws Exception
    {
        final String networkName = "etcd_test";

        pullLatestImage();
        ensureNetworkExists(networkName);

        final DockerContainerInstance[] instances =
                new DockerContainerInstance[count];
        final Map<String, String> nameHosts = new HashMap<>();

        final int startIndex = (count == 1) ? 0 : 1;
        final int endIndex = (count == 1) ? count : (count + 1);

        for (int i = startIndex; i < endIndex; i++)
        {
            nameHosts.put("etcd" + i, "localhost");
        }

        final String clusterString = getClusterString(nameHosts);

        int currIndex = 0;
        for (final Map.Entry<String, String> entry : nameHosts.entrySet())
        {
            final String name = entry.getKey();
            final String host = entry.getValue();
            final String portPrefix = (count == 1) ? "" : ((currIndex + 1) + "");

            final String[] command = new String[] {
                    "docker", "run", "-d", "--name", name,
                    "--net=" + networkName,
                    //                "-v /usr/share/ca-certificates/:/etc/ssl/certs",
                    "-p", portPrefix + "4001:4001", "-p",
                    portPrefix + "2380:2380", "-p", portPrefix + "2379:2379",
                    getETCDDockerImageName(), "etcd",
                    "-name", name,
                    "-advertise-client-urls",
                    "http://" + name + ":2379,http://" + name + ":4001",
                    "-listen-client-urls",
                    "http://" + name + ":2379,http://" + name + ":4001",
                    "-initial-advertise-peer-urls", "http://" + name + ":2380",
                    "-listen-peer-urls", "http://" + name + ":2380",
                    "-initial-cluster-token", "etcd-cluster-1",
                    "-initial-cluster", clusterString,
                    "-initial-cluster-state", "new"
            };

            final InputStream inputStream = runDockerCommand(command);

            // Allow each container to start up.  Not sleeping here causes the
            // yet-to-be ready cluster to fail adding a node.
            Thread.sleep(2500L);

            instances[currIndex++] =
                    new DockerContainerInstance(this,
                                                readMessage(inputStream).toCharArray(),
                                                host + ":" + portPrefix
                                                + "2379");
        }

        return instances;
    }

    private String getClusterString(final Map<String, String> nameHosts)
    {
        final StringBuilder clusterString = new StringBuilder();
        for (final Map.Entry<String, String> entry : nameHosts.entrySet())
        {
            final String name = entry.getKey();
            clusterString.append(name).append("=http://").append(name)
                    .append(":2380,");
        }

        clusterString.deleteCharAt(clusterString.lastIndexOf(","));

        return clusterString.toString();
    }

    public String readMessage(final InputStream inputStream) throws IOException
    {
        final BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder message = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null)
        {
            message.append(line).append("\n");
        }

        bufferedReader.close();
        return message.toString().trim();
    }
}
