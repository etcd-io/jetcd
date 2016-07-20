// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package com.coreos.jetcd;

/**
 * Protobuf type {@code jetcd.PutResponse}
 */
public  final class PutResponse extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:jetcd.PutResponse)
    PutResponseOrBuilder {
  // Use PutResponse.newBuilder() to construct.
  private PutResponse(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private PutResponse() {
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private PutResponse(
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
            com.coreos.jetcd.KeyValue.Builder subBuilder = null;
            if (prevKv_ != null) {
              subBuilder = prevKv_.toBuilder();
            }
            prevKv_ = input.readMessage(com.coreos.jetcd.KeyValue.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(prevKv_);
              prevKv_ = subBuilder.buildPartial();
            }

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
    return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_PutResponse_descriptor;
  }

  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_PutResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.coreos.jetcd.PutResponse.class, com.coreos.jetcd.PutResponse.Builder.class);
  }

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

  public static final int PREV_KV_FIELD_NUMBER = 2;
  private com.coreos.jetcd.KeyValue prevKv_;
  /**
   * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
   *
   * <pre>
   * if prev_kv is set in the request, the previous key-value pair will be returned.
   * </pre>
   */
  public boolean hasPrevKv() {
    return prevKv_ != null;
  }
  /**
   * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
   *
   * <pre>
   * if prev_kv is set in the request, the previous key-value pair will be returned.
   * </pre>
   */
  public com.coreos.jetcd.KeyValue getPrevKv() {
    return prevKv_ == null ? com.coreos.jetcd.KeyValue.getDefaultInstance() : prevKv_;
  }
  /**
   * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
   *
   * <pre>
   * if prev_kv is set in the request, the previous key-value pair will be returned.
   * </pre>
   */
  public com.coreos.jetcd.KeyValueOrBuilder getPrevKvOrBuilder() {
    return getPrevKv();
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
    if (prevKv_ != null) {
      output.writeMessage(2, getPrevKv());
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
    if (prevKv_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(2, getPrevKv());
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  public static com.coreos.jetcd.PutResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.coreos.jetcd.PutResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.coreos.jetcd.PutResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.coreos.jetcd.PutResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.coreos.jetcd.PutResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static com.coreos.jetcd.PutResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static com.coreos.jetcd.PutResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static com.coreos.jetcd.PutResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static com.coreos.jetcd.PutResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static com.coreos.jetcd.PutResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.coreos.jetcd.PutResponse prototype) {
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
   * Protobuf type {@code jetcd.PutResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:jetcd.PutResponse)
      com.coreos.jetcd.PutResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_PutResponse_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_PutResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.coreos.jetcd.PutResponse.class, com.coreos.jetcd.PutResponse.Builder.class);
    }

    // Construct using com.coreos.jetcd.PutResponse.newBuilder()
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
      if (prevKvBuilder_ == null) {
        prevKv_ = null;
      } else {
        prevKv_ = null;
        prevKvBuilder_ = null;
      }
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_PutResponse_descriptor;
    }

    public com.coreos.jetcd.PutResponse getDefaultInstanceForType() {
      return com.coreos.jetcd.PutResponse.getDefaultInstance();
    }

    public com.coreos.jetcd.PutResponse build() {
      com.coreos.jetcd.PutResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public com.coreos.jetcd.PutResponse buildPartial() {
      com.coreos.jetcd.PutResponse result = new com.coreos.jetcd.PutResponse(this);
      if (headerBuilder_ == null) {
        result.header_ = header_;
      } else {
        result.header_ = headerBuilder_.build();
      }
      if (prevKvBuilder_ == null) {
        result.prevKv_ = prevKv_;
      } else {
        result.prevKv_ = prevKvBuilder_.build();
      }
      onBuilt();
      return result;
    }

    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.coreos.jetcd.PutResponse) {
        return mergeFrom((com.coreos.jetcd.PutResponse)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.coreos.jetcd.PutResponse other) {
      if (other == com.coreos.jetcd.PutResponse.getDefaultInstance()) return this;
      if (other.hasHeader()) {
        mergeHeader(other.getHeader());
      }
      if (other.hasPrevKv()) {
        mergePrevKv(other.getPrevKv());
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
      com.coreos.jetcd.PutResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.coreos.jetcd.PutResponse) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

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

    private com.coreos.jetcd.KeyValue prevKv_ = null;
    private com.google.protobuf.SingleFieldBuilder<
        com.coreos.jetcd.KeyValue, com.coreos.jetcd.KeyValue.Builder, com.coreos.jetcd.KeyValueOrBuilder> prevKvBuilder_;
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    public boolean hasPrevKv() {
      return prevKvBuilder_ != null || prevKv_ != null;
    }
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    public com.coreos.jetcd.KeyValue getPrevKv() {
      if (prevKvBuilder_ == null) {
        return prevKv_ == null ? com.coreos.jetcd.KeyValue.getDefaultInstance() : prevKv_;
      } else {
        return prevKvBuilder_.getMessage();
      }
    }
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    public Builder setPrevKv(com.coreos.jetcd.KeyValue value) {
      if (prevKvBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        prevKv_ = value;
        onChanged();
      } else {
        prevKvBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    public Builder setPrevKv(
        com.coreos.jetcd.KeyValue.Builder builderForValue) {
      if (prevKvBuilder_ == null) {
        prevKv_ = builderForValue.build();
        onChanged();
      } else {
        prevKvBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    public Builder mergePrevKv(com.coreos.jetcd.KeyValue value) {
      if (prevKvBuilder_ == null) {
        if (prevKv_ != null) {
          prevKv_ =
            com.coreos.jetcd.KeyValue.newBuilder(prevKv_).mergeFrom(value).buildPartial();
        } else {
          prevKv_ = value;
        }
        onChanged();
      } else {
        prevKvBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    public Builder clearPrevKv() {
      if (prevKvBuilder_ == null) {
        prevKv_ = null;
        onChanged();
      } else {
        prevKv_ = null;
        prevKvBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    public com.coreos.jetcd.KeyValue.Builder getPrevKvBuilder() {
      
      onChanged();
      return getPrevKvFieldBuilder().getBuilder();
    }
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    public com.coreos.jetcd.KeyValueOrBuilder getPrevKvOrBuilder() {
      if (prevKvBuilder_ != null) {
        return prevKvBuilder_.getMessageOrBuilder();
      } else {
        return prevKv_ == null ?
            com.coreos.jetcd.KeyValue.getDefaultInstance() : prevKv_;
      }
    }
    /**
     * <code>optional .jetcd.KeyValue prev_kv = 2;</code>
     *
     * <pre>
     * if prev_kv is set in the request, the previous key-value pair will be returned.
     * </pre>
     */
    private com.google.protobuf.SingleFieldBuilder<
        com.coreos.jetcd.KeyValue, com.coreos.jetcd.KeyValue.Builder, com.coreos.jetcd.KeyValueOrBuilder> 
        getPrevKvFieldBuilder() {
      if (prevKvBuilder_ == null) {
        prevKvBuilder_ = new com.google.protobuf.SingleFieldBuilder<
            com.coreos.jetcd.KeyValue, com.coreos.jetcd.KeyValue.Builder, com.coreos.jetcd.KeyValueOrBuilder>(
                getPrevKv(),
                getParentForChildren(),
                isClean());
        prevKv_ = null;
      }
      return prevKvBuilder_;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }


    // @@protoc_insertion_point(builder_scope:jetcd.PutResponse)
  }

  // @@protoc_insertion_point(class_scope:jetcd.PutResponse)
  private static final com.coreos.jetcd.PutResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.coreos.jetcd.PutResponse();
  }

  public static com.coreos.jetcd.PutResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PutResponse>
      PARSER = new com.google.protobuf.AbstractParser<PutResponse>() {
    public PutResponse parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      try {
        return new PutResponse(input, extensionRegistry);
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

  public static com.google.protobuf.Parser<PutResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<PutResponse> getParserForType() {
    return PARSER;
  }

  public com.coreos.jetcd.PutResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

