package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.ClientBuilder;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClientBuilderTest {

  private ClientBuilder builder;

  private Assertion test;

  @BeforeMethod
  public void setup() {
    builder = ClientBuilder.newBuilder();
    test = new Assertion();
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testEndPoints_Null() {
    builder.setEndpoints(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEndPoints_Verify_Empty() {
    builder.setEndpoints("");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEndPoints_Verify_EmptyAfterTrim() {
    builder.setEndpoints(" ");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEndPoints_Verify_SomeEmpty() {
    builder.setEndpoints("127.0.0.1:2379", " ");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testBuild_WithoutEndpoints() {
    builder.build();
  }

  @Test
  public void testBuild_WithValidFormatEndpoints() {
    builder.setEndpoints("127.0.0.1:2379", "http://localhost:1984", "http://www.foo.com:2300/",
                         "http://192.168.1.10:8888/");

    test.assertEquals(builder.getEndpoints().size(), 4);

    test.assertTrue(builder.getEndpoints().contains("127.0.0.1:2379"));
    test.assertTrue(builder.getEndpoints().contains("http://localhost:1984"));
    test.assertTrue(builder.getEndpoints().contains("http://www.foo.com:2300/"));
    test.assertTrue(builder.getEndpoints().contains("http://192.168.1.10:8888/"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuild_WithInvalidIpEndpoint() {
      builder.setEndpoints("xxx.yyy.111.222:8999");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuild_WithInvalidPortEndpoint() {
    builder.setEndpoints("222.255.111.222:uyw3");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBuild_WithoutPortEndpoint() {
    builder.setEndpoints("222.255.111.222");
  }
}
