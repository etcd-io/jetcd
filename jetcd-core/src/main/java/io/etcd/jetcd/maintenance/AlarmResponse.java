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

import java.util.List;
import java.util.stream.Collectors;

import io.etcd.jetcd.AbstractResponse;
import io.etcd.jetcd.Maintenance;

/**
 * AlarmResponse returned by {@link Maintenance#listAlarms()} contains a header
 * and a list of AlarmMember.
 */
public class AlarmResponse extends AbstractResponse<io.etcd.jetcd.api.AlarmResponse> {

    private List<AlarmMember> alarms;

    public AlarmResponse(io.etcd.jetcd.api.AlarmResponse response) {
        super(response, response.getHeader());
    }

    private static AlarmMember toAlarmMember(io.etcd.jetcd.api.AlarmMember alarmMember) {
        io.etcd.jetcd.maintenance.AlarmType type;
        switch (alarmMember.getAlarm()) {
            case NONE:
                type = io.etcd.jetcd.maintenance.AlarmType.NONE;
                break;
            case NOSPACE:
                type = io.etcd.jetcd.maintenance.AlarmType.NOSPACE;
                break;
            default:
                type = io.etcd.jetcd.maintenance.AlarmType.UNRECOGNIZED;
        }
        return new AlarmMember(alarmMember.getMemberID(), type);
    }

    /**
     * @return a list of alarms associated with the alarm request.
     */
    public synchronized List<AlarmMember> getAlarms() {
        if (alarms == null) {
            alarms = getResponse().getAlarmsList().stream().map(AlarmResponse::toAlarmMember).collect(Collectors.toList());
        }

        return alarms;
    }
}
