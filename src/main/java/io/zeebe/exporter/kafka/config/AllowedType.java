package io.zeebe.exporter.kafka.config;

public enum AllowedType {
  NONE(0, "none"),
  COMMAND(1, "command"),
  EVENT(2, "event"),
  REJECTION(3, "rejection"),
  ALL(4, "all");

  public String name;
  public int id;

  AllowedType(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public static AllowedType forName(String name) {
    if (NONE.name.equals(name))
      return NONE;
    else if (COMMAND.name.equals(name))
      return COMMAND;
    else if (EVENT.name.equals(name))
      return EVENT;
    else if (REJECTION.name.equals(name))
      return REJECTION;
    else if (ALL.name.equals(name))
      return ALL;
    else
      throw new IllegalArgumentException("Unknown record type name: " + name);
  }
}
