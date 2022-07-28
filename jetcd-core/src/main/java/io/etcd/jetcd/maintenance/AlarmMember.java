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

package io.etcd.jetcd.maintenance;

public class AlarmMember {

    private long memberId;
    private AlarmType alarmType;

    public AlarmMember(long memberId, AlarmType alarmType) {
        this.memberId = memberId;
        this.alarmType = alarmType;
    }

    /**
     * Returns the ID of the member associated with the raised alarm.
     *
     * @return the member id.
     */
    public long getMemberId() {
        return memberId;
    }

    /**
     *
     * Returns the type of alarm which has been raised.
     *
     * @return the alarm type.
     */
    public AlarmType getAlarmType() {
        return alarmType;
    }
}
