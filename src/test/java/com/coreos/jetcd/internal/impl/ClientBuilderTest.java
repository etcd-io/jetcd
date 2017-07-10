package com.coreos.jetcd.internal.impl;

import com.coreos.jetcd.ClientBuilder;
import org.testng.annotations.BeforeMethod;
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
}
