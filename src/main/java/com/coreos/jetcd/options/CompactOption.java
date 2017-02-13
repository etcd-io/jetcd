package com.coreos.jetcd.options;

public final class CompactOption {

  public static final CompactOption DEFAULT = newBuilder().build();

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private long revision = 0L;
    private boolean physical = false;

    private Builder() {
    }

    /**
     * Provide the revision to use for the compact request.
     * <p>All superseded keys with a revision less than the compaction revision will be removed..
     *
     * @param revision the revision to compact.
     * @return builder
     */
    public Builder withRevision(long revision) {
      this.revision = revision;
      return this;
    }

    /**
     * Set the compact request to wait until the compaction is physically applied.
     *
     * @param physical whether the compact should wait until physically applied
     * @return builder
     */
    public Builder withPhysical(boolean physical) {
      this.physical = physical;
      return this;
    }

    public CompactOption build() {
      return new CompactOption(this.revision, this.physical);
    }
  }

  private final long revision;
  private final boolean physical;

  private CompactOption(long revision, boolean physical) {
    this.revision = revision;
    this.physical = physical;
  }

  public long getRevision() {
    return revision;
  }

  public boolean isPhysical() {
    return physical;
  }
}
