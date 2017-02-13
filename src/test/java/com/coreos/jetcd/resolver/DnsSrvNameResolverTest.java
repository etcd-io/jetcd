package com.coreos.jetcd.resolver;

import io.grpc.Attributes;
import io.grpc.internal.GrpcUtil;
import java.net.URI;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

public class DnsSrvNameResolverTest {

  private Assertion test = new Assertion();

  @Test
  public void testResolver() {
    DnsSrvNameResolver discovery = new DnsSrvNameResolver("_xmpp-server._tcp.gmail.com",
        GrpcUtil.SHARED_CHANNEL_EXECUTOR);

    test.assertFalse(discovery.getServers().isEmpty());
  }

  @Test
  public void testResolverFactory() throws Exception {
    test.assertEquals("_etcd-server._tcp.my-domain-1.com",
        ((DnsSrvNameResolver) new DnsSrvNameResolverFactory().newNameResolver(
            URI.create("dns+srv:///my-domain-1.com"), Attributes.EMPTY)).getName());

    test.assertEquals(
        "_etcd-server._tcp.my-domain-2.com",
        ((DnsSrvNameResolver) new DnsSrvNameResolverFactory().newNameResolver(
            URI.create("dns+srv:///_etcd-server._tcp.my-domain-2.com"), Attributes.EMPTY))
            .getName());
  }
}
