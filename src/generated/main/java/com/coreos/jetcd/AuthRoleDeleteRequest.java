// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package com.coreos.jetcd;

/**
 * Protobuf type {@code jetcd.AuthRoleDeleteRequest}
 */
public  final class AuthRoleDeleteRequest extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:jetcd.AuthRoleDeleteRequest)
    AuthRoleDeleteRequestOrBuilder {
  // Use AuthRoleDeleteRequest.newBuilder() to construct.
  private AuthRoleDeleteRequest(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private AuthRoleDeleteRequest() {
    role_ = "";
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private AuthRoleDeleteRequest(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) {
    this();
    int mutable_bitField0_ = 0;
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!input.skipField(tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            role_ = s;
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw new RuntimeException(e.setUnfinishedMessage(this));
    } catch (java.io.IOException e) {
      throw new RuntimeException(
          new com.google.protobuf.InvalidProtocolBufferException(
              e.getMessage()).setUnfinishedMessage(this));
    } finally {
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthRoleDeleteRequest_descriptor;
  }

  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthRoleDeleteRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.coreos.jetcd.AuthRoleDeleteRequest.class, com.coreos.jetcd.AuthRoleDeleteRequest.Builder.class);
  }

  public static final int ROLE_FIELD_NUMBER = 1;
  private volatile java.lang.Object role_;
  /**
   * <code>optional string role = 1;</code>
   */
  public java.lang.String getRole() {
    java.lang.Object ref = role_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      role_ = s;
      return s;
    }
  }
  /**
   * <code>optional string role = 1;</code>
   */
  public com.google.protobuf.ByteString
      getRoleBytes() {
    java.lang.Object ref = role_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      role_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!getRoleBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessage.writeString(output, 1, role_);
    }
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getRoleBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessage.computeStringSize(1, role_);
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static com.coreos.jetcd.AuthRoleDeleteRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.coreos.jetcd.AuthRoleDeleteRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessage.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code jetcd.AuthRoleDeleteRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:jetcd.AuthRoleDeleteRequest)
      com.coreos.jetcd.AuthRoleDeleteRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthRoleDeleteRequest_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthRoleDeleteRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.coreos.jetcd.AuthRoleDeleteRequest.class, com.coreos.jetcd.AuthRoleDeleteRequest.Builder.class);
    }

    // Construct using com.coreos.jetcd.AuthRoleDeleteRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      role_ = "";

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthRoleDeleteRequest_descriptor;
    }

    public com.coreos.jetcd.AuthRoleDeleteRequest getDefaultInstanceForType() {
      return com.coreos.jetcd.AuthRoleDeleteRequest.getDefaultInstance();
    }

    public com.coreos.jetcd.AuthRoleDeleteRequest build() {
      com.coreos.jetcd.AuthRoleDeleteRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public com.coreos.jetcd.AuthRoleDeleteRequest buildPartial() {
      com.coreos.jetcd.AuthRoleDeleteRequest result = new com.coreos.jetcd.AuthRoleDeleteRequest(this);
      result.role_ = role_;
      onBuilt();
      return result;
    }

    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.coreos.jetcd.AuthRoleDeleteRequest) {
        return mergeFrom((com.coreos.jetcd.AuthRoleDeleteRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.coreos.jetcd.AuthRoleDeleteRequest other) {
      if (other == com.coreos.jetcd.AuthRoleDeleteRequest.getDefaultInstance()) return this;
      if (!other.getRole().isEmpty()) {
        role_ = other.role_;
        onChanged();
      }
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.coreos.jetcd.AuthRoleDeleteRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.coreos.jetcd.AuthRoleDeleteRequest) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object role_ = "";
    /**
     * <code>optional string role = 1;</code>
     */
    public java.lang.String getRole() {
      java.lang.Object ref = role_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        role_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>optional string role = 1;</code>
     */
    public com.google.protobuf.ByteString
        getRoleBytes() {
      java.lang.Object ref = role_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        role_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>optional string role = 1;</code>
     */
    public Builder setRole(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      role_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>optional string role = 1;</code>
     */
    public Builder clearRole() {
      
      role_ = getDefaultInstance().getRole();
      onChanged();
      return this;
    }
    /**
     * <code>optional string role = 1;</code>
     */
    public Builder setRoleBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      role_ = value;
      onChanged();
      return this;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }


    // @@protoc_insertion_point(builder_scope:jetcd.AuthRoleDeleteRequest)
  }

  // @@protoc_insertion_point(class_scope:jetcd.AuthRoleDeleteRequest)
  private static final com.coreos.jetcd.AuthRoleDeleteRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.coreos.jetcd.AuthRoleDeleteRequest();
  }

  public static com.coreos.jetcd.AuthRoleDeleteRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<AuthRoleDeleteRequest>
      PARSER = new com.google.protobuf.AbstractParser<AuthRoleDeleteRequest>() {
    public AuthRoleDeleteRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      try {
        return new AuthRoleDeleteRequest(input, extensionRegistry);
      } catch (RuntimeException e) {
        if (e.getCause() instanceof
            com.google.protobuf.InvalidProtocolBufferException) {
          throw (com.google.protobuf.InvalidProtocolBufferException)
              e.getCause();
        }
        throw e;
      }
    }
  };

  public static com.google.protobuf.Parser<AuthRoleDeleteRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<AuthRoleDeleteRequest> getParserForType() {
    return PARSER;
  }

  public com.coreos.jetcd.AuthRoleDeleteRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

