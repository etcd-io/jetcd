package com.coreos.jetcd.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.kv.PutResponse;
import io.grpc.PickFirstBalancerFactory;
import io.grpc.util.RoundRobinLoadBalancerFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * KV service test cases.
 */
public class LoadBalancerTest {
  @Rule
  public Timeout timeout = Timeout.seconds(10);

  @Test
  public void testPickFirstBalancerFactory() throws Exception {
    Client client = ClientBuilder.newBuilder()
        .setEndpoints(TestConstants.endpoints)
        .setLoadBalancerFactory(PickFirstBalancerFactory.getInstance())
        .build();

    KV kv = client.getKVClient();

    PutResponse response;
    long lastMemberId = 0;

    try {
      for (int i = 0; i < TestConstants.endpoints.length * 2; i++) {
        response = kv.put(TestUtil.randomByteSequence(), TestUtil.randomByteSequence()).get();

        if (i == 0) {
          lastMemberId = response.getHeader().getMemberId();
        }

        assertThat(response.getHeader().getMemberId()).isEqualTo(lastMemberId);
      }
    } finally {
      kv.close();
      client.close();
    }
  }

  @Test
  public void testRoundRobinLoadBalancerFactory() throws Exception {
    Client client = ClientBuilder.newBuilder()
        .setEndpoints(TestConstants.endpoints)
        .setLoadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
        .build();

    KV kv = client.getKVClient();

    PutResponse response;
    long lastMemberId = 0;
    long differences = 0;

    try {
      for (int i = 0; i < TestConstants.endpoints.length; i++) {
        response = kv.put(TestUtil.randomByteSequence(), TestUtil.randomByteSequence()).get();

        if (i > 0 && lastMemberId != response.getHeader().getMemberId()) {
          differences++;
        }

        lastMemberId = response.getHeader().getMemberId();
      }

      assertThat(differences).isNotEqualTo(lastMemberId);
    } finally {
      kv.close();
      client.close();
    }
  }
}
