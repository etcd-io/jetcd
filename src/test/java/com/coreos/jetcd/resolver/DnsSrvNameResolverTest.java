package com.coreos.jetcd.resolver;

import java.net.URI;

import io.grpc.Attributes;
import io.grpc.internal.GrpcUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DnsSrvNameResolverTest {
    @Test(enabled = true)
    public void testResolver() {
        DnsSrvNameResolver discovery = new DnsSrvNameResolver(
            "_xmpp-server._tcp.gmail.com",
            GrpcUtil.SHARED_CHANNEL_EXECUTOR
        );


        Assert.assertFalse(discovery.getServers().isEmpty());
    }

    @Test(enabled = true)
    public void testResolverFactory() throws Exception {
        Assert.assertEquals(
            "_etcd-server._tcp.my-domain-1.com",
            ((DnsSrvNameResolver) new DnsSrvNameResolverFactory().newNameResolver(
                URI.create("dns+srv:///my-domain-1.com"), Attributes.EMPTY))
                .getName()
        );

        Assert.assertEquals(
            "_etcd-server._tcp.my-domain-2.com",
            ((DnsSrvNameResolver) new DnsSrvNameResolverFactory().newNameResolver(
                URI.create("dns+srv:///_etcd-server._tcp.my-domain-2.com"), Attributes.EMPTY))
                .getName()
        );
    }
}
