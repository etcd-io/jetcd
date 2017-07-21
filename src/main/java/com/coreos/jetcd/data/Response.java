package com.coreos.jetcd.data;


/**
 * represents a generic Jetcd response.
 */
public interface Response {

  Header getHeader();

  interface Header {

    long getClusterId();

    long getMemberId();

    long getRevision();

    long getRaftTerm();
  }
}
