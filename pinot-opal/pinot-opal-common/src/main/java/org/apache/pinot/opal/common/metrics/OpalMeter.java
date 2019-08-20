/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.opal.common.metrics;

import org.apache.pinot.common.Utils;
import org.apache.pinot.common.metrics.AbstractMetrics;

public enum OpalMeter implements AbstractMetrics.Meter {
  INGESTION_COUNT("ingestion count"),

  // consumer metrics
  MESSAGE_INGEST_COUNT_PER_BATCH("message"),

  // segment updater metrics
  MESSAGE_FETCH_PER_ROUND("messages");

  ;


  private final String meterName;
  private final String unit;

  OpalMeter(String unit) {
    this.unit = unit;
    this.meterName = Utils.toCamelCase(name().toLowerCase());
  }

  @Override
  public String getMeterName() {
    return meterName;
  }

  @Override
  public String getUnit() {
    return unit;
  }

  @Override
  public boolean isGlobal() {
    return true;
  }
}
