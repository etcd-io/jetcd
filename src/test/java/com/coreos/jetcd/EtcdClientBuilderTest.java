package com.coreos.jetcd;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EtcdClientBuilderTest {

    private EtcdClientBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {
        builder = EtcdClientBuilder.newBuilder();
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "please set etcd host and port")
    public void missingEtcdSettings() {
        builder.build();
    }
}