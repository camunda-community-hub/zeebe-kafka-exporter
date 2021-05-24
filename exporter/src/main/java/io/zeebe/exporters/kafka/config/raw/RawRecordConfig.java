/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
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
package io.zeebe.exporters.kafka.config.raw;

@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public final class RawRecordConfig {

  /**
   * Type is a comma separated string of accepted record types, allowing you to filter if you want
   * nothing (""), commands ("command"), events ("events"), or rejections ("rejection"), or a
   * combination of the three, e.g. "command,event".
   */
  public String type;

  /**
   * Topic is the topic to which the record with the given value type should be sent to, e.g. for a
   * deployment record below we would send the record to "zeebe-deployment" topic.
   */
  public String topic;
}
