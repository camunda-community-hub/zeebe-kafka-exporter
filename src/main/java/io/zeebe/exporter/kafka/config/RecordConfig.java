/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package io.zeebe.exporter.kafka.config;

import io.zeebe.protocol.clientapi.RecordType;
import java.util.EnumSet;

public class RecordConfig {
  public static final String DEFAULT_TOPIC_NAME = "zeebe";
  public static final EnumSet<RecordType> DEFAULT_ALLOWED_TYPES = EnumSet.allOf(RecordType.class);

  public EnumSet<RecordType> allowedTypes = DEFAULT_ALLOWED_TYPES;
  public String topic = DEFAULT_TOPIC_NAME;
}
