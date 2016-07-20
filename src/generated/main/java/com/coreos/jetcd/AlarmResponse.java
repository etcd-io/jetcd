// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package com.coreos.jetcd;

/**
 * Protobuf type {@code jetcd.AlarmResponse}
 */
public  final class AlarmResponse extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:jetcd.AlarmResponse)
    AlarmResponseOrBuilder {
  // Use AlarmResponse.newBuilder() to construct.
  private AlarmResponse(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private AlarmResponse() {
    alarms_ = java.util.Collections.emptyList();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private AlarmResponse(
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
            if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
              alarms_ = new java.util.ArrayList<com.coreos.jetcd.AlarmMember>();
              mutable_bitField0_ |= 0x00000002;
            }
            alarms_.add(input.readMessage(com.coreos.jetcd.AlarmMember.parser(), extensionRegistry));
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
        alarms_ = java.util.Collections.unmodifiableList(alarms_);
      }
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AlarmResponse_descriptor;
  }

  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AlarmResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.coreos.jetcd.AlarmResponse.class, com.coreos.jetcd.AlarmResponse.Builder.class);
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

  public static final int ALARMS_FIELD_NUMBER = 2;
  private java.util.List<com.coreos.jetcd.AlarmMember> alarms_;
  /**
   * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
   *
   * <pre>
   * alarms is a list of alarms associated with the alarm request.
   * </pre>
   */
  public java.util.List<com.coreos.jetcd.AlarmMember> getAlarmsList() {
    return alarms_;
  }
  /**
   * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
   *
   * <pre>
   * alarms is a list of alarms associated with the alarm request.
   * </pre>
   */
  public java.util.List<? extends com.coreos.jetcd.AlarmMemberOrBuilder> 
      getAlarmsOrBuilderList() {
    return alarms_;
  }
  /**
   * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
   *
   * <pre>
   * alarms is a list of alarms associated with the alarm request.
   * </pre>
   */
  public int getAlarmsCount() {
    return alarms_.size();
  }
  /**
   * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
   *
   * <pre>
   * alarms is a list of alarms associated with the alarm request.
   * </pre>
   */
  public com.coreos.jetcd.AlarmMember getAlarms(int index) {
    return alarms_.get(index);
  }
  /**
   * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
   *
   * <pre>
   * alarms is a list of alarms associated with the alarm request.
   * </pre>
   */
  public com.coreos.jetcd.AlarmMemberOrBuilder getAlarmsOrBuilder(
      int index) {
    return alarms_.get(index);
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
    for (int i = 0; i < alarms_.size(); i++) {
      output.writeMessage(2, alarms_.get(i));
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
    for (int i = 0; i < alarms_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(2, alarms_.get(i));
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  public static com.coreos.jetcd.AlarmResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.coreos.jetcd.AlarmResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.coreos.jetcd.AlarmResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.coreos.jetcd.AlarmResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.coreos.jetcd.AlarmResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static com.coreos.jetcd.AlarmResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static com.coreos.jetcd.AlarmResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static com.coreos.jetcd.AlarmResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static com.coreos.jetcd.AlarmResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static com.coreos.jetcd.AlarmResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.coreos.jetcd.AlarmResponse prototype) {
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
   * Protobuf type {@code jetcd.AlarmResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:jetcd.AlarmResponse)
      com.coreos.jetcd.AlarmResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AlarmResponse_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AlarmResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.coreos.jetcd.AlarmResponse.class, com.coreos.jetcd.AlarmResponse.Builder.class);
    }

    // Construct using com.coreos.jetcd.AlarmResponse.newBuilder()
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
        getAlarmsFieldBuilder();
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
      if (alarmsBuilder_ == null) {
        alarms_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000002);
      } else {
        alarmsBuilder_.clear();
      }
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.coreos.jetcd.EtcdJavaProto.internal_static_jetcd_AlarmResponse_descriptor;
    }

    public com.coreos.jetcd.AlarmResponse getDefaultInstanceForType() {
      return com.coreos.jetcd.AlarmResponse.getDefaultInstance();
    }

    public com.coreos.jetcd.AlarmResponse build() {
      com.coreos.jetcd.AlarmResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public com.coreos.jetcd.AlarmResponse buildPartial() {
      com.coreos.jetcd.AlarmResponse result = new com.coreos.jetcd.AlarmResponse(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (headerBuilder_ == null) {
        result.header_ = header_;
      } else {
        result.header_ = headerBuilder_.build();
      }
      if (alarmsBuilder_ == null) {
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          alarms_ = java.util.Collections.unmodifiableList(alarms_);
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.alarms_ = alarms_;
      } else {
        result.alarms_ = alarmsBuilder_.build();
      }
      result.bitField0_ = to_bitField0_;
      onBuilt();
      return result;
    }

    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.coreos.jetcd.AlarmResponse) {
        return mergeFrom((com.coreos.jetcd.AlarmResponse)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.coreos.jetcd.AlarmResponse other) {
      if (other == com.coreos.jetcd.AlarmResponse.getDefaultInstance()) return this;
      if (other.hasHeader()) {
        mergeHeader(other.getHeader());
      }
      if (alarmsBuilder_ == null) {
        if (!other.alarms_.isEmpty()) {
          if (alarms_.isEmpty()) {
            alarms_ = other.alarms_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureAlarmsIsMutable();
            alarms_.addAll(other.alarms_);
          }
          onChanged();
        }
      } else {
        if (!other.alarms_.isEmpty()) {
          if (alarmsBuilder_.isEmpty()) {
            alarmsBuilder_.dispose();
            alarmsBuilder_ = null;
            alarms_ = other.alarms_;
            bitField0_ = (bitField0_ & ~0x00000002);
            alarmsBuilder_ = 
              com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                 getAlarmsFieldBuilder() : null;
          } else {
            alarmsBuilder_.addAllMessages(other.alarms_);
          }
        }
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
      com.coreos.jetcd.AlarmResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (com.coreos.jetcd.AlarmResponse) e.getUnfinishedMessage();
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

    private java.util.List<com.coreos.jetcd.AlarmMember> alarms_ =
      java.util.Collections.emptyList();
    private void ensureAlarmsIsMutable() {
      if (!((bitField0_ & 0x00000002) == 0x00000002)) {
        alarms_ = new java.util.ArrayList<com.coreos.jetcd.AlarmMember>(alarms_);
        bitField0_ |= 0x00000002;
       }
    }

    private com.google.protobuf.RepeatedFieldBuilder<
        com.coreos.jetcd.AlarmMember, com.coreos.jetcd.AlarmMember.Builder, com.coreos.jetcd.AlarmMemberOrBuilder> alarmsBuilder_;

    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public java.util.List<com.coreos.jetcd.AlarmMember> getAlarmsList() {
      if (alarmsBuilder_ == null) {
        return java.util.Collections.unmodifiableList(alarms_);
      } else {
        return alarmsBuilder_.getMessageList();
      }
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public int getAlarmsCount() {
      if (alarmsBuilder_ == null) {
        return alarms_.size();
      } else {
        return alarmsBuilder_.getCount();
      }
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public com.coreos.jetcd.AlarmMember getAlarms(int index) {
      if (alarmsBuilder_ == null) {
        return alarms_.get(index);
      } else {
        return alarmsBuilder_.getMessage(index);
      }
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder setAlarms(
        int index, com.coreos.jetcd.AlarmMember value) {
      if (alarmsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureAlarmsIsMutable();
        alarms_.set(index, value);
        onChanged();
      } else {
        alarmsBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder setAlarms(
        int index, com.coreos.jetcd.AlarmMember.Builder builderForValue) {
      if (alarmsBuilder_ == null) {
        ensureAlarmsIsMutable();
        alarms_.set(index, builderForValue.build());
        onChanged();
      } else {
        alarmsBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder addAlarms(com.coreos.jetcd.AlarmMember value) {
      if (alarmsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureAlarmsIsMutable();
        alarms_.add(value);
        onChanged();
      } else {
        alarmsBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder addAlarms(
        int index, com.coreos.jetcd.AlarmMember value) {
      if (alarmsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureAlarmsIsMutable();
        alarms_.add(index, value);
        onChanged();
      } else {
        alarmsBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder addAlarms(
        com.coreos.jetcd.AlarmMember.Builder builderForValue) {
      if (alarmsBuilder_ == null) {
        ensureAlarmsIsMutable();
        alarms_.add(builderForValue.build());
        onChanged();
      } else {
        alarmsBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder addAlarms(
        int index, com.coreos.jetcd.AlarmMember.Builder builderForValue) {
      if (alarmsBuilder_ == null) {
        ensureAlarmsIsMutable();
        alarms_.add(index, builderForValue.build());
        onChanged();
      } else {
        alarmsBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder addAllAlarms(
        java.lang.Iterable<? extends com.coreos.jetcd.AlarmMember> values) {
      if (alarmsBuilder_ == null) {
        ensureAlarmsIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, alarms_);
        onChanged();
      } else {
        alarmsBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder clearAlarms() {
      if (alarmsBuilder_ == null) {
        alarms_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
      } else {
        alarmsBuilder_.clear();
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public Builder removeAlarms(int index) {
      if (alarmsBuilder_ == null) {
        ensureAlarmsIsMutable();
        alarms_.remove(index);
        onChanged();
      } else {
        alarmsBuilder_.remove(index);
      }
      return this;
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public com.coreos.jetcd.AlarmMember.Builder getAlarmsBuilder(
        int index) {
      return getAlarmsFieldBuilder().getBuilder(index);
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public com.coreos.jetcd.AlarmMemberOrBuilder getAlarmsOrBuilder(
        int index) {
      if (alarmsBuilder_ == null) {
        return alarms_.get(index);  } else {
        return alarmsBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public java.util.List<? extends com.coreos.jetcd.AlarmMemberOrBuilder> 
         getAlarmsOrBuilderList() {
      if (alarmsBuilder_ != null) {
        return alarmsBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(alarms_);
      }
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public com.coreos.jetcd.AlarmMember.Builder addAlarmsBuilder() {
      return getAlarmsFieldBuilder().addBuilder(
          com.coreos.jetcd.AlarmMember.getDefaultInstance());
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public com.coreos.jetcd.AlarmMember.Builder addAlarmsBuilder(
        int index) {
      return getAlarmsFieldBuilder().addBuilder(
          index, com.coreos.jetcd.AlarmMember.getDefaultInstance());
    }
    /**
     * <code>repeated .jetcd.AlarmMember alarms = 2;</code>
     *
     * <pre>
     * alarms is a list of alarms associated with the alarm request.
     * </pre>
     */
    public java.util.List<com.coreos.jetcd.AlarmMember.Builder> 
         getAlarmsBuilderList() {
      return getAlarmsFieldBuilder().getBuilderList();
    }
    private com.google.protobuf.RepeatedFieldBuilder<
        com.coreos.jetcd.AlarmMember, com.coreos.jetcd.AlarmMember.Builder, com.coreos.jetcd.AlarmMemberOrBuilder> 
        getAlarmsFieldBuilder() {
      if (alarmsBuilder_ == null) {
        alarmsBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
            com.coreos.jetcd.AlarmMember, com.coreos.jetcd.AlarmMember.Builder, com.coreos.jetcd.AlarmMemberOrBuilder>(
                alarms_,
                ((bitField0_ & 0x00000002) == 0x00000002),
                getParentForChildren(),
                isClean());
        alarms_ = null;
      }
      return alarmsBuilder_;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }


    // @@protoc_insertion_point(builder_scope:jetcd.AlarmResponse)
  }

  // @@protoc_insertion_point(class_scope:jetcd.AlarmResponse)
  private static final com.coreos.jetcd.AlarmResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.coreos.jetcd.AlarmResponse();
  }

  public static com.coreos.jetcd.AlarmResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<AlarmResponse>
      PARSER = new com.google.protobuf.AbstractParser<AlarmResponse>() {
    public AlarmResponse parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      try {
        return new AlarmResponse(input, extensionRegistry);
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

  public static com.google.protobuf.Parser<AlarmResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<AlarmResponse> getParserForType() {
    return PARSER;
  }

  public com.coreos.jetcd.AlarmResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

