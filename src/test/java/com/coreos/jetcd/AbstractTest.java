package com.coreos.jetcd;

abstract class AbstractTest
{
    private static final String SINGLE_ENDPOINT_PROPERTY_KEY =
            "SINGLE_ENDPOINT";
    private static final String CLUSTER_ENDPOINTS_PROPERTY_KEY =
            "CLUSTER_ENDPOINTS";


    /**
     * Obtain the endpoints from already running Etcd instance(s).
     * @return      String[] endpoint array, never null.
     */
    String getConfiguredSingleEndpoint()
    {
        final String endpointProperty =
                System.getProperty(SINGLE_ENDPOINT_PROPERTY_KEY);

        return (endpointProperty == null) ? null : endpointProperty;
    }

    String[] getConfiguredClusterEndpoints()
    {
        final String clusterEndpointPropertyValue =
                System.getProperty(CLUSTER_ENDPOINTS_PROPERTY_KEY);

        return (clusterEndpointPropertyValue == null)
               ? null : clusterEndpointPropertyValue.split(",");
    }
}
