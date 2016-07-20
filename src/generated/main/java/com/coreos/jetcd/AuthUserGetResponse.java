// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package com.coreos.jetcd;

/**
 * Protobuf type {@code jetcd.AuthUserGetResponse}
 */
public  final class AuthUserGetResponse extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:jetcd.AuthUserGetResponse)
    AuthUserGetResponseOrBuilder {
  // Use AuthUserGetResponse.newBuilder() to construct.
  private AuthUserGetResponse(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private AuthUserGetResponse() {
    roles_ = com.google.protobuf.LazyStringArrayList.EMPTY;
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private AuthUserGetResponse(
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
            com.coreos.jetcd.ResponseHeader.Builder subBuilder = null;
            if (header_ != null) {
              subBuilder = header_.toBuilder();
            }
            header_ = input.readMessage(com.coreos.jetcd.ResponseHeader.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(header_);
              header_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();
            if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
              roles_ = new com.google.protobuf.LazyStringArrayList();
              mutable_bitField0_ |= 0x00000002;
            }
            roles_.add(s);
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
      if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
        roles_ = roles_.getUnmodifiableView();
      }
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthUserGetResponse_descriptor;
  }

  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthUserGetResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.coreos.jetcd.AuthUserGetResponse.class, com.coreos.jetcd.AuthUserGetResponse.Builder.class);
  }

  private int bitField0_;
  public static final int HEADER_FIELD_NUMBER = 1;
  private com.coreos.jetcd.ResponseHeader header_;
  /**
   * <code>optional .jetcd.ResponseHeader header = 1;</code>
   */
  public boolean hasHeader() {
    return header_ != null;
  }
  /**
   * <code>optional .jetcd.ResponseHeader header = 1;</code>
   */
  public com.coreos.jetcd.ResponseHeader getHeader() {
    return header_ == null ? com.coreos.jetcd.ResponseHeader.getDefaultInstance() : header_;
  }
  /**
   * <code>optional .jetcd.ResponseHeader header = 1;</code>
   */
  public com.coreos.jetcd.ResponseHeaderOrBuilder getHeaderOrBuilder() {
    return getHeader();
  }

  public static final int ROLES_FIELD_NUMBER = 2;
  private com.google.protobuf.LazyStringList roles_;
  /**
   * <code>repeated string roles = 2;</code>
   */
  public com.google.protobuf.ProtocolStringList
      getRolesList() {
    return roles_;
  }
  /**
   * <code>repeated string roles = 2;</code>
   */
  public int getRolesCount() {
    return roles_.size();
  }
  /**
   * <code>repeated string roles = 2;</code>
   */
  public java.lang.String getRoles(int index) {
    return roles_.get(index);
  }
  /**
   * <code>repeated string roles = 2;</code>
   */
  public com.google.protobuf.ByteString
      getRolesBytes(int index) {
    return roles_.getByteString(index);
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
    if (header_ != null) {
      output.writeMessage(1, getHeader());
    }
    for (int i = 0; i < roles_.size(); i++) {
      com.google.protobuf.GeneratedMessage.writeString(output, 2, roles_.getRaw(i));
    }
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (header_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getHeader());
    }
    {
      int dataSize = 0;
      for (int i = 0; i < roles_.size(); i++) {
        dataSize += computeStringSizeNoTag(roles_.getRaw(i));
      }
      size += dataSize;
      size += 1 * getRolesList().size();
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  public static com.coreos.jetcd.AuthUserGetResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static com.coreos.jetcd.AuthUserGetResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.coreos.jetcd.AuthUserGetResponse prototype) {
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
   * Protobuf type {@code jetcd.AuthUserGetResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:jetcd.AuthUserGetResponse)
      com.coreos.jetcd.AuthUserGetResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthUserGetResponse_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthUserGetResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.coreos.jetcd.AuthUserGetResponse.class, com.coreos.jetcd.AuthUserGetResponse.Builder.class);
    }

    // Construct using com.coreos.jetcd.AuthUserGetResponse.newBuilder()
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
      if (headerBuilder_ == null) {
        header_ = null;
      } else {
        header_ = null;
        headerBuilder_ = null;
      }
      roles_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000002);
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AuthUserGetResponse_descriptor;
    }

    public com.coreos.jetcd.AuthUserGetResponse getDefaultInstanceForType() {
      return com.coreos.jetcd.AuthUserGetResponse.getDefaultInstance();
    }

    public com.coreos.jetcd.AuthUserGetResponse build() {
      com.coreos.jetcd.AuthUserGetResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public com.coreos.jetcd.AuthUserGetResponse buildPartial() {
      com.coreos.jetcd.AuthUserGetResponse result = new com.coreos.jetcd.AuthUserGetResponse(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (headerBuilder_ == null) {
        result.header_ = header_;
      } else {
        result.header_ = headerBuilder_.build();
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        roles_ = roles_.getUnmodifiableView();
        bitField0_ = (bitField0_ & ~0x00000002);
      }
      result.roles_ = roles_;
      result.bitField0_ = to_bitField0_;
      onBuilt();
      return result;
    }

    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.coreos.jetcd.AuthUserGetResponse) {
        return mergeFrom((com.coreos.jetcd.AuthUserGetResponse)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.coreos.jetcd.AuthUserGetResponse other) {
      if (other == com.coreos.jetcd.AuthUserGetResponse.getDefaultInstance()) return this;
      if (other.hasHeader()) {
        mergeHeader(other.getHeader());
      }
      if (!other.roles_.isEmpty()) {
        if (roles_.isEmpty()) {
          roles_ = other.roles_;
          bitField0_ = (bitField0_ & ~0x00000002);
        } else {
          ensureRolesIsMutable();
          roles_.addAll(other.roles_);
        }
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
      com.coreos.jetcd.AuthUserGetResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.coreos.jetcd.AuthUserGetResponse) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private com.coreos.jetcd.ResponseHeader header_ = null;
    private com.google.protobuf.SingleFieldBuilder<
        com.coreos.jetcd.ResponseHeader, com.coreos.jetcd.ResponseHeader.Builder, com.coreos.jetcd.ResponseHeaderOrBuilder> headerBuilder_;
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    public boolean hasHeader() {
      return headerBuilder_ != null || header_ != null;
    }
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    public com.coreos.jetcd.ResponseHeader getHeader() {
      if (headerBuilder_ == null) {
        return header_ == null ? com.coreos.jetcd.ResponseHeader.getDefaultInstance() : header_;
      } else {
        return headerBuilder_.getMessage();
      }
    }
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    public Builder setHeader(com.coreos.jetcd.ResponseHeader value) {
      if (headerBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        header_ = value;
        onChanged();
      } else {
        headerBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    public Builder setHeader(
        com.coreos.jetcd.ResponseHeader.Builder builderForValue) {
      if (headerBuilder_ == null) {
        header_ = builderForValue.build();
        onChanged();
      } else {
        headerBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    public Builder mergeHeader(com.coreos.jetcd.ResponseHeader value) {
      if (headerBuilder_ == null) {
        if (header_ != null) {
          header_ =
            com.coreos.jetcd.ResponseHeader.newBuilder(header_).mergeFrom(value).buildPartial();
        } else {
          header_ = value;
        }
        onChanged();
      } else {
        headerBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    public Builder clearHeader() {
      if (headerBuilder_ == null) {
        header_ = null;
        onChanged();
      } else {
        header_ = null;
        headerBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    public com.coreos.jetcd.ResponseHeader.Builder getHeaderBuilder() {
      
      onChanged();
      return getHeaderFieldBuilder().getBuilder();
    }
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    public com.coreos.jetcd.ResponseHeaderOrBuilder getHeaderOrBuilder() {
      if (headerBuilder_ != null) {
        return headerBuilder_.getMessageOrBuilder();
      } else {
        return header_ == null ?
            com.coreos.jetcd.ResponseHeader.getDefaultInstance() : header_;
      }
    }
    /**
     * <code>optional .jetcd.ResponseHeader header = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilder<
        com.coreos.jetcd.ResponseHeader, com.coreos.jetcd.ResponseHeader.Builder, com.coreos.jetcd.ResponseHeaderOrBuilder> 
        getHeaderFieldBuilder() {
      if (headerBuilder_ == null) {
        headerBuilder_ = new com.google.protobuf.SingleFieldBuilder<
            com.coreos.jetcd.ResponseHeader, com.coreos.jetcd.ResponseHeader.Builder, com.coreos.jetcd.ResponseHeaderOrBuilder>(
                getHeader(),
                getParentForChildren(),
                isClean());
        header_ = null;
      }
      return headerBuilder_;
    }

    private com.google.protobuf.LazyStringList roles_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    private void ensureRolesIsMutable() {
      if (!((bitField0_ & 0x00000002) == 0x00000002)) {
        roles_ = new com.google.protobuf.LazyStringArrayList(roles_);
        bitField0_ |= 0x00000002;
       }
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList
        getRolesList() {
      return roles_.getUnmodifiableView();
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public int getRolesCount() {
      return roles_.size();
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public java.lang.String getRoles(int index) {
      return roles_.get(index);
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public com.google.protobuf.ByteString
        getRolesBytes(int index) {
      return roles_.getByteString(index);
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public Builder setRoles(
        int index, java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureRolesIsMutable();
      roles_.set(index, value);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public Builder addRoles(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureRolesIsMutable();
      roles_.add(value);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public Builder addAllRoles(
        java.lang.Iterable<java.lang.String> values) {
      ensureRolesIsMutable();
      com.google.protobuf.AbstractMessageLite.Builder.addAll(
          values, roles_);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public Builder clearRoles() {
      roles_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000002);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string roles = 2;</code>
     */
    public Builder addRolesBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      ensureRolesIsMutable();
      roles_.add(value);
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


    // @@protoc_insertion_point(builder_scope:jetcd.AuthUserGetResponse)
  }

  // @@protoc_insertion_point(class_scope:jetcd.AuthUserGetResponse)
  private static final com.coreos.jetcd.AuthUserGetResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.coreos.jetcd.AuthUserGetResponse();
  }

  public static com.coreos.jetcd.AuthUserGetResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<AuthUserGetResponse>
      PARSER = new com.google.protobuf.AbstractParser<AuthUserGetResponse>() {
    public AuthUserGetResponse parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      try {
        return new AuthUserGetResponse(input, extensionRegistry);
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

  public static com.google.protobuf.Parser<AuthUserGetResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<AuthUserGetResponse> getParserForType() {
    return PARSER;
  }

  public com.coreos.jetcd.AuthUserGetResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

