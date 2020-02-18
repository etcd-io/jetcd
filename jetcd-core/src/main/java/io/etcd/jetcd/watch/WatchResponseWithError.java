/*
 * Copyright 2016-2020 The jetcd authors
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

package io.etcd.jetcd.watch;

import io.etcd.jetcd.common.exception.EtcdException;

public class WatchResponseWithError {

  private WatchResponse watchResponse;
  private EtcdException exception;

  private WatchResponseWithError() {

  }

  public WatchResponseWithError(WatchResponse watchResponse) {
    this.watchResponse = watchResponse;
  }

  public WatchResponseWithError(EtcdException e) {
    this.exception = e;
  }


  public WatchResponse getWatchResponse() {
    return watchResponse;
  }

  public EtcdException getException() {
    return exception;
  }
}
