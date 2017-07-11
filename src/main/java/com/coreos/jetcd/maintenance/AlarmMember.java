package com.coreos.jetcd.maintenance;

public class AlarmMember {

  private long memberId;
  private AlarmType alarmType;

  public AlarmMember(long memberId, AlarmType alarmType) {
    this.memberId = memberId;
    this.alarmType = alarmType;
  }

  /**
   * returns the ID of the member associated with the raised alarm.
   */
  public long getMemberId() {
    return memberId;
  }

  /**
   * returns the type of alarm which has been raised.
   */
  public AlarmType getAlarmType() {
    return alarmType;
  }
}
