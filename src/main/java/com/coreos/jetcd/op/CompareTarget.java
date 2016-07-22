package com.coreos.jetcd.op;

import com.coreos.jetcd.api.Compare;
import com.google.protobuf.ByteString;

/**
 * Compare target used in {@link Txn}
 *
 * @param <T>
 */
public abstract class CompareTarget<T> {

    /**
     * Compare on a given <i>version</i>.
     *
     * @param version version to compare
     * @return the version compare target
     */
    public static VersionCompareTarget version(long version) {
        return new VersionCompareTarget(version);
    }

    /**
     * Compare on the create <i>revision</i>.
     *
     * @param revision the create revision
     * @return the create revision compare target
     */
    public static CreateRevisionCompareTarget createRevision(long revision) {
        return new CreateRevisionCompareTarget(revision);
    }

    /**
     * Compare on the modification <i>revision</i>.
     *
     * @param revision the modification revision
     * @return the modification revision compare target
     */
    public static ModRevisionCompareTarget modRevision(long revision) {
        return new ModRevisionCompareTarget(revision);
    }

    /**
     * Compare on the <i>value</i>.
     *
     * @param value the value to compare
     * @return the value compare target
     */
    public static ValueCompareTarget value(ByteString value) {
        return new ValueCompareTarget(value);
    }

    private final Compare.CompareTarget target;
    private final T targetValue;

    protected CompareTarget(Compare.CompareTarget target, T targetValue) {
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

    private static final class VersionCompareTarget extends CompareTarget<Long> {
        VersionCompareTarget(Long targetValue) {
            super(Compare.CompareTarget.VERSION, targetValue);
        }
    }

    private static final class CreateRevisionCompareTarget extends CompareTarget<Long> {
        CreateRevisionCompareTarget(Long targetValue) {
            super(Compare.CompareTarget.CREATE, targetValue);
        }
    }

    private static final class ModRevisionCompareTarget extends CompareTarget<Long> {
        ModRevisionCompareTarget(Long targetValue) {
            super(Compare.CompareTarget.MOD, targetValue);
        }
    }

    private static final class ValueCompareTarget extends CompareTarget<ByteString> {
        ValueCompareTarget(ByteString targetValue) {
            super(Compare.CompareTarget.VALUE, targetValue);
        }
    }

}
