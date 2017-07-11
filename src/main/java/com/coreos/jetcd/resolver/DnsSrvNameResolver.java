package com.coreos.jetcd.resolver;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.EquivalentAddressGroup;
import io.grpc.internal.SharedResourceHolder;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

final class DnsSrvNameResolver extends AbstractEtcdNameResolver {
  private static final String[] ATTRIBUTE_IDS;
  private static final Hashtable<String, String> ENV;

  static {
    ATTRIBUTE_IDS = new String[]{"SRV"};

    ENV = new Hashtable<>();
    ENV.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
    ENV.put("java.naming.provider.url", "dns:");
  }

  private final String name;

  public DnsSrvNameResolver(String name,
      SharedResourceHolder.Resource<ExecutorService> executorResource) {
    super(name, executorResource);
    this.name = name;
  }

  @Override
  protected EquivalentAddressGroup getAddressGroup() throws Exception {
    DirContext ctx = new InitialDirContext(ENV);
    NamingEnumeration<?> resolved = ctx.getAttributes(name, ATTRIBUTE_IDS).get("srv").getAll();
    List<SocketAddress> servers = new LinkedList<>();

    while (resolved.hasMore()) {
      servers.add(srvRecordToAddress((String) resolved.next()));
    }

    return new EquivalentAddressGroup(servers);
  }

  @VisibleForTesting
  protected String getName() {
    return name;
  }

  private SocketAddress srvRecordToAddress(String dnsSrvRecord) {
    String[] split = dnsSrvRecord.split(" ");
    return new InetSocketAddress(split[3].trim(), Integer.parseInt(split[2].trim()));
  }
}
