package com.coreos.jetcd.op;

import com.coreos.jetcd.api.Compare;
import com.google.protobuf.ByteString;

/**
 * Cmp target used in {@link Txn}
 *
 * @param <T>
 */
public abstract class CmpTarget<T> {

    /**
     * Cmp on a given <i>version</i>.
     *
     * @param version version to compare
     * @return the version compare target
     */
    public static VersionCmpTarget version(long version) {
        return new VersionCmpTarget(version);
    }

    /**
     * Cmp on the create <i>revision</i>.
     *
     * @param revision the create revision
     * @return the create revision compare target
     */
    public static CreateRevisionCmpTarget createRevision(long revision) {
        return new CreateRevisionCmpTarget(revision);
    }

    /**
     * Cmp on the modification <i>revision</i>.
     *
     * @param revision the modification revision
     * @return the modification revision compare target
     */
    public static ModRevisionCmpTarget modRevision(long revision) {
        return new ModRevisionCmpTarget(revision);
    }

    /**
     * Cmp on the <i>value</i>.
     *
     * @param value the value to compare
     * @return the value compare target
     */
    public static ValueCmpTarget value(ByteString value) {
        return new ValueCmpTarget(value);
    }

    private final Compare.CompareTarget target;
    private final T                     targetValue;

    protected CmpTarget(Compare.CompareTarget target, T targetValue) {
        this.target = target;
        this.targetValue = targetValue;
    }

    /**
     * Get the compare target used for this compare.
     *
     * @return the compare target used for this compare
     */
    public Compare.CompareTarget getTarget() {
        return target;
    }

    /**
     * Get the compare target value of this compare.
     *
     * @return the compare target value of this compare.
     */
    public T getTargetValue() {
        return targetValue;
    }

    private static final class VersionCmpTarget extends CmpTarget<Long> {
        VersionCmpTarget(Long targetValue) {
            super(Compare.CompareTarget.VERSION, targetValue);
        }
    }

    private static final class CreateRevisionCmpTarget extends CmpTarget<Long> {
        CreateRevisionCmpTarget(Long targetValue) {
            super(Compare.CompareTarget.CREATE, targetValue);
        }
    }

    private static final class ModRevisionCmpTarget extends CmpTarget<Long> {
        ModRevisionCmpTarget(Long targetValue) {
            super(Compare.CompareTarget.MOD, targetValue);
        }
    }

    private static final class ValueCmpTarget extends CmpTarget<ByteString> {
        ValueCmpTarget(ByteString targetValue) {
            super(Compare.CompareTarget.VALUE, targetValue);
        }
    }

}
