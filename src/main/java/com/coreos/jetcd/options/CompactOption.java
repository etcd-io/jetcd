package com.coreos.jetcd.options;

public final class CompactOption {

  public static final CompactOption DEFAULT = newBuilder().build();

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private boolean physical = false;

    private Builder() {
    }

    /**
     * make compact RPC call wait until
     * the compaction is physically applied to the local database
     * such that compacted entries are totally removed from the
     * backend database.
     *
     * @param physical whether the compact should wait until physically applied
     * @return builder
     */
    public Builder withCompactPhysical(boolean physical) {
      this.physical = physical;
      return this;
    }

    public CompactOption build() {
      return new CompactOption(this.physical);
    }
  }

  private final boolean physical;

  private CompactOption(boolean physical) {
    this.physical = physical;
  }

  public boolean isPhysical() {
    return physical;
  }
}
