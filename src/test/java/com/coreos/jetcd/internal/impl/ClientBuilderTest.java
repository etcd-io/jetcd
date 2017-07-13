package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.ClientBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ClientBuilderTest {

  private ClientBuilder builder;

  @BeforeMethod
  public void setup() {
    builder = ClientBuilder.newBuilder();
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

  @Test(dataProvider = "validEndpoints")
  public void testBuild_WithValidFormatEndpoints(String endpoint) {
    builder.setEndpoints(endpoint);
  }

  @DataProvider
  private Object[][] validEndpoints() {
    return new Object[][] {
        {"http://[fe80::6e40:8ff:fe]:2380"},
        {"http://127.0.0.1:2379"},
        {"http://localhost:1984"},
        {"http://www.foo.com:2300"},
        {"http://192.168.1.10:8888"}
    };
  }

  @Test(dataProvider = "invalidEndpoints", expectedExceptions = IllegalArgumentException.class)
  public void testBuild_WithInValidFormatEndpoints(String endpoint) {
    builder.setEndpoints(endpoint);
  }

  @DataProvider
  private Object[][] invalidEndpoints() {
    return new Object[][] {
        {"://127.0.0.1:2379"},
        {"mailto://127.0.0.1:2379"},
        {"http://127.0.0.1"},
        {"http://127.0.0.1:2379/path"}
    };
  }
}
