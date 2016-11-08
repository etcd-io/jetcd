package com.coreos.jetcd.integration;


public interface EtcdInstance
{
    /**
     * Obtain the endpoint for this instance.
     * @return      String endpoint (scheme://host:port/path)
     */
    String getEndpoint();

    /**
     * Destroy this instance.
     * @throws Exception
     */
    void destroy() throws Exception;
}
