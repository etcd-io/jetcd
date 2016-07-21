package com.coreos.jetcd.op;

import com.coreos.jetcd.api.Compare;

public abstract class CompareTarget<T> {

    public static VersionCompareTarget version(long version) {
        return new VersionCompareTarget(Compare.CompareTarget.VERSION, version);
    }

    // TODO: add revision, value compare targets

    private final Compare.CompareTarget target;
    private final T targetValue;

    protected CompareTarget(Compare.CompareTarget target, T targetValue) {
        this.target = target;
        this.targetValue = targetValue;
    }

    private static final class VersionCompareTarget extends CompareTarget<Long> {
        VersionCompareTarget(Compare.CompareTarget target, Long targetValue) {
            super(target, targetValue);
        }
    }

}
