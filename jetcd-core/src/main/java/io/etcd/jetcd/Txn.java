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

package io.etcd.jetcd;

import java.util.concurrent.CompletableFuture;

import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.Op;

/**
 * Txn is the interface that wraps mini-transactions.
 *
 * <h3>Usage examples</h3>
 *
 * <pre>
 * {@code
 * txn.If(
 *    new Cmp(KEY, Cmp.Op.GREATER, CmpTarget.value(VALUE)),
 *    new Cmp(KEY, cmp.Op.EQUAL, CmpTarget.version(2))
 * ).Then(
 *    Op.put(KEY2, VALUE2, PutOption.DEFAULT),
 *    Op.put(KEY3, VALUE3, PutOption.DEFAULT)
 * ).Else(
 *    Op.put(KEY4, VALUE4, PutOption.DEFAULT),
 *    Op.put(KEY4, VALUE4, PutOption.DEFAULT)
 * ).commit();
 * }
 * </pre>
 *
 * <p>
 * Txn also supports If, Then, and Else chaining. e.g.
 *
 * <pre>
 * {@code
 * txn.If(
 *    new Cmp(KEY, Cmp.Op.GREATER, CmpTarget.value(VALUE))
 * ).If(
 *    new Cmp(KEY, cmp.Op.EQUAL, CmpTarget.version(VERSION))
 * ).Then(
 *    Op.put(KEY2, VALUE2, PutOption.DEFAULT)
 * ).Then(
 *    Op.put(KEY3, VALUE3, PutOption.DEFAULT)
 * ).Else(
 *    Op.put(KEY4, VALUE4, PutOption.DEFAULT)
 * ).Else(
 *    Op.put(KEY4, VALUE4, PutOption.DEFAULT)
 * ).commit();
 * }
 * </pre>
 */
public interface Txn {
  //CHECKSTYLE:OFF
  /**
   * takes a list of comparison. If all comparisons passed in succeed,
   * the operations passed into Then() will be executed. Or the operations
   * passed into Else() will be executed.
   *
   * @param cmps the comparisons
   * @return this object
   */
  Txn If(Cmp... cmps);
  //CHECKSTYLE:ON

  //CHECKSTYLE:OFF
  /**
   * takes a list of operations. The Ops list will be executed, if the
   * comparisons passed in If() succeed.
   *
   * @param ops the operations
   * @return this object
   */
  Txn Then(Op... ops);
  //CHECKSTYLE:ON

  //CHECKSTYLE:OFF
  /**
   * takes a list of operations. The Ops list will be executed, if the
   * comparisons passed in If() fail.
   *
   * @param ops the operations
   * @return this object
   */
  Txn Else(Op... ops);
  //CHECKSTYLE:ON

    /**
     * tries to commit the transaction.
     *
     * @return a TxnResponse wrapped in CompletableFuture
     */
    CompletableFuture<TxnResponse> commit();
}
