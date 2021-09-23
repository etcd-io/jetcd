/*
 * Copyright 2016-2021 The jetcd authors
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

package io.etcd.jetcd.op;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.api.Compare;

import com.google.protobuf.ByteString;

/**
 * Cmp target used in {@link io.etcd.jetcd.Txn}.
 */
public abstract class CmpTarget<T> {

    /**
     * Cmp on a given <i>version</i>.
     *
     * @param  version version to compare
     * @return         the version compare target
     */
    public static VersionCmpTarget version(long version) {
        return new VersionCmpTarget(version);
    }

    /**
     * Cmp on the create <i>revision</i>.
     *
     * @param  revision the create revision
     * @return          the create revision compare target
     */
    public static CreateRevisionCmpTarget createRevision(long revision) {
        return new CreateRevisionCmpTarget(revision);
    }

    /**
     * Cmp on the modification <i>revision</i>.
     *
     * @param  revision the modification revision
     * @return          the modification revision compare target
     */
    public static ModRevisionCmpTarget modRevision(long revision) {
        return new ModRevisionCmpTarget(revision);
    }

    /**
     * Cmp on the <i>value</i>.
     *
     * @param  value the value to compare
     * @return       the value compare target
     */
    public static ValueCmpTarget value(ByteSequence value) {
        return new ValueCmpTarget(ByteString.copyFrom(value.getBytes()));
    }

    private final Compare.CompareTarget target;
    private final T targetValue;

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

    public static final class VersionCmpTarget extends CmpTarget<Long> {

        VersionCmpTarget(Long targetValue) {
            super(Compare.CompareTarget.VERSION, targetValue);
        }
    }

    public static final class CreateRevisionCmpTarget extends CmpTarget<Long> {

        CreateRevisionCmpTarget(Long targetValue) {
            super(Compare.CompareTarget.CREATE, targetValue);
        }
    }

    public static final class ModRevisionCmpTarget extends CmpTarget<Long> {

        ModRevisionCmpTarget(Long targetValue) {
            super(Compare.CompareTarget.MOD, targetValue);
        }
    }

    public static final class ValueCmpTarget extends CmpTarget<ByteString> {

        ValueCmpTarget(ByteString targetValue) {
            super(Compare.CompareTarget.VALUE, targetValue);
        }
    }

}
