package com.coreos.jetcd.resolver;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Attributes;
import io.grpc.ResolvedServerInfo;
import io.grpc.internal.SharedResourceHolder;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DnsSrvNameResolver extends AbstractEtcdNameResolver {

  private static final Logger LOGGER;
  private static final String[] ATTRIBUTE_IDS;
  private static final Hashtable<String, String> ENV;

  static {
    LOGGER = LoggerFactory.getLogger(DnsSrvNameResolver.class);
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
  protected List<ResolvedServerInfo> getServers() {
    try {
      DirContext ctx = new InitialDirContext(ENV);
      NamingEnumeration<?> resolved = ctx.getAttributes(name, ATTRIBUTE_IDS).get("srv").getAll();
      List<ResolvedServerInfo> servers = new LinkedList<>();

      while (resolved.hasMore()) {
        servers.add(srvRecordToServerInfo((String) resolved.next()));
      }

      return servers;
    } catch (Exception e) {
      LOGGER.warn("", e);
    }

    return Collections.emptyList();
  }

  @VisibleForTesting
  protected String getName() {
    return name;
  }

  private InetSocketAddress srvRecordToAddress(String dnsSrvRecord) {
    String[] split = dnsSrvRecord.split(" ");
    return new InetSocketAddress(split[3].trim(), Integer.parseInt(split[2].trim()));
  }

  private ResolvedServerInfo srvRecordToServerInfo(String dnsSrvRecord) {
    return new ResolvedServerInfo(srvRecordToAddress(dnsSrvRecord), Attributes.EMPTY);
  }
}
