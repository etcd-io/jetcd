package com.coreos.jetcd.maintenance;

/**
 * represents type of alarm which can be raised.
 */
public enum AlarmType {
  NONE, // default, used to query if any alarm is active
  NOSPACE, // space quota is exhausted
  UNRECOGNIZED,
}
