package com.coreos.jetcd.integration;

import com.coreos.jetcd.DockerCommandRunner;

import java.io.InputStream;
import java.util.logging.Logger;


public class DockerContainerInstance implements EtcdInstance
{
    private final Logger LOGGER =
            Logger.getLogger(DockerContainerInstance.class.getName());
    private final DockerCommandRunner commandRunner;
    private final char[] hash;
    private final String endpoint;


    public DockerContainerInstance(final DockerCommandRunner dockerCommandRunner,
                                   final char[] hash,
                                   final String endpoint)
    {
        this.commandRunner = dockerCommandRunner;
        this.hash = hash;
        this.endpoint = endpoint;
    }

    @Override
    public void destroy() throws Exception
    {
        final String hashString = String.valueOf(hash);

        LOGGER.info("\nDestroying " + hashString + "\n");
        commandRunner.runDockerCommand("docker", "stop", hashString);
        final InputStream inputStream =
                commandRunner.runDockerCommand("docker", "rm", hashString);

        LOGGER.info("\n" + commandRunner.readMessage(inputStream) + "\n");
    }

    @Override
    public String getEndpoint()
    {
        return endpoint;
    }
}
