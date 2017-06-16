package com.coreos.jetcd.options;

public class LeaseOption {

  public static final LeaseOption DEFAULT = newBuilder().build();

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private boolean attachedKeys;

    private Builder() {
    }

    /**
     * requests lease timeToLive API to return attached keys of given lease ID.
     *
     * @return builder.
     */
    public Builder withAttachedKeys() {
      this.attachedKeys = true;
      return this;
    }

    public LeaseOption build() {
      return new LeaseOption(this.attachedKeys);
    }
  }

  private final boolean attachedKeys;

  private LeaseOption(boolean attachedKeys) {
    this.attachedKeys = attachedKeys;
  }

  public boolean isAttachedKeys() {
    return attachedKeys;
  }

}
