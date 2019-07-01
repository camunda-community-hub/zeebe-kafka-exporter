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
package io.zeebe.exporters.kafka.record;

import io.zeebe.protocol.RecordType;

public enum AllowedType {
  COMMAND("command", RecordType.COMMAND),
  EVENT("event", RecordType.EVENT),
  REJECTION("rejection", RecordType.COMMAND_REJECTION);

  public String name;
  public RecordType recordType;

  AllowedType(String name, RecordType recordType) {
    this.name = name;
    this.recordType = recordType;
  }

  public static AllowedType forName(String name) {
    if (COMMAND.name.equals(name)) {
      return COMMAND;
    } else if (EVENT.name.equals(name)) {
      return EVENT;
    } else if (REJECTION.name.equals(name)) {
      return REJECTION;
    } else {
      throw new IllegalArgumentException("Unknown record type name: " + name);
    }
  }
}
