package com.coreos.jetcd.maintenance;

import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.data.AbstractResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AlarmResponse returned by {@link Maintenance#listAlarms()} contains a header
 * and a list of AlarmMember.
 */
public class AlarmResponse extends AbstractResponse<com.coreos.jetcd.api.AlarmResponse> {

  private List<AlarmMember> alarms;

  public AlarmResponse(com.coreos.jetcd.api.AlarmResponse response) {
    super(response, response.getHeader());
  }

  private static AlarmMember toAlarmMember(com.coreos.jetcd.api.AlarmMember alarmMember) {
    com.coreos.jetcd.maintenance.AlarmType type;
    switch (alarmMember.getAlarm()) {
      case NONE:
        type = com.coreos.jetcd.maintenance.AlarmType.NONE;
        break;
      case NOSPACE:
        type = com.coreos.jetcd.maintenance.AlarmType.NOSPACE;
        break;
      default:
        type = com.coreos.jetcd.maintenance.AlarmType.UNRECOGNIZED;
    }
    return new AlarmMember(alarmMember.getMemberID(), type);
  }

  /**
   * returns a list of alarms associated with the alarm request.
   */
  public synchronized List<AlarmMember> getAlarms() {
    if (alarms == null) {
      alarms = getResponse().getAlarmsList().stream()
          .map(AlarmResponse::toAlarmMember)
          .collect(Collectors.toList());
    }

    return alarms;
  }
}
