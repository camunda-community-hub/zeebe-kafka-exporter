package io.zeebe.exporter.kafka.record;

import io.zeebe.exporter.record.Record;

import java.util.function.Predicate;

@FunctionalInterface
public interface RecordTester extends Predicate<Record<?>> {
}
