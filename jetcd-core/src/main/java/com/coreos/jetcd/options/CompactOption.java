/**
 * Copyright 2017 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
