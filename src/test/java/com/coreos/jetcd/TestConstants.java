package com.coreos.jetcd;


/**
 * Test constants, contain the cluster info.
 */
public class TestConstants
{
    public static final String[] ENDPOINTS = new String[]{
            "http://localhost:2379", "http://localhost:22379",
            "http://localhost:32379"
    };

    public static final String[] PEER_URLS = new String[]{
            "http://localhost:12380", "http://localhost:22380",
            "http://localhost:32380"
    };
}
