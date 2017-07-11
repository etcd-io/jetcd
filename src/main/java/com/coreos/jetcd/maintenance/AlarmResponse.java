package com.coreos.jetcd.maintenance;

import com.coreos.jetcd.Maintenance;
import com.coreos.jetcd.data.Header;
import java.util.List;

/**
 * AlarmResponse returned by {@link Maintenance#listAlarms()} contains a header
 * and a list of AlarmMember.
 */
public class AlarmResponse {

  private final Header header;
  private final List<AlarmMember> alarms;

  public AlarmResponse(Header header, List<AlarmMember> alarms) {
    this.header = header;
    this.alarms = alarms;
  }

  public Header getHeader() {
    return header;
  }

  /**
   * returns a list of alarms associated with the alarm request.
   */
  public List<AlarmMember> getAlarms() {
    return alarms;
  }
}
