package com.coreos.jetcd.integration;


/**
 * Represents an already running Etcd instance.
 */
public class ExternalInstance implements EtcdInstance
{
    private final String endpoint;


    public ExternalInstance(String endpoint)
    {
        this.endpoint = endpoint;
    }


    /**
     * Obtain the endpoint for this instance.
     *
     * @return String endpoint (scheme://host:port/path)
     */
    @Override
    public String getEndpoint()
    {
        return endpoint;
    }

    /**
     * Destroy this instance.
     *
     * @throws Exception            For any errors.
     */
    @Override
    public void destroy() throws Exception
    {
        System.out.println(
                String.format("Can't destroy the already running instance at '%s'.",
                              endpoint));
    }
}
