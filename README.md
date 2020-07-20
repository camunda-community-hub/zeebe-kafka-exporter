Zeebe Kafka Exporter
=====================

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.zeebe%3Azeebe-kafka-exporter-root&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.zeebe%3Azeebe-kafka-exporter-root)

An easy to use exporter which will export Zeebe records to a configured Kafka topic, compatible with
[zeebe](https://github.com/zeebe-io/zeebe) versions:

- [0.23.4]https://github.com/zeebe-io/zeebe/releases/tag/0.23.4)
- [0.24.1](https://github.com/zeebe-io/zeebe/releases/tag/0.24.1)

For more information about the exporters please read the [Exporter documentation](https://docs.zeebe.io/basics/exporters.html).

## Trying it out

If you just want to try it out immediately, go to the [Docker](#docker) section below - this will
show you how to quickly spawn all required services and see how they interact together.

## Installation

The quickest way to get started is:

1. Download the latest release (`zeebe-kafka-exporter-*-jar-with-dependencies.jar`).
1. Copy it to the `lib/` folder of your Zeebe brokers.
1. Copy the contents of the configuration from `exporter.yml` into your Zeebe `application.yml`
1. Update the configuration's list of servers to point to your Kafka instances.

The next time you start your Zeebe cluster, all event-type records will be exported to their respective Kafka topics.

## Usage

The exporter is set up to stream records from Zeebe to Kafka as they are processed by the exporter stream processor.
While this is done asynchronously, to ensure that the position is updated correctly, it keeps buffers in flight requests
and processes their results in the order they were sent, not necessarily in the order the Kafka cluster answered.

Records are serialized to Kafka using plain JSON. Keys are JSON representation of
`io.zeebe.exporters.kafka.serde.RecordId`, and values are serialized using the standard Zeebe
`io.zeebe.protocol.record.Record#toJson()` method. The `io.zeebe.exporters:zeebe-kafka-exporter-serde`
module provides easy to use `Deserializer` implementations in Java for use in your consumers.

The [configuration file](https://github.com/zeebe-io/zeebe-kafka-exporter/blob/master/exporter/exporter.yml)
is a good starting point to learn more about how the exporter works.

### Advanced configuration

You can configure the producer for more advanced use cases by using the
`zeebe.broker.exporters.kafka.args.producer.config` configuration property, which lets you
arbitrarily configure your Kafka producer the same way you normally would. This property is parsed
as a standard Java properties file. For example, say you wanted to connect to a secured Kafka
instance, you could define the producer config as:

```yaml
config: |
security.protocol=SSL
ssl.truststore.location=/truststore.jks
ssl.truststore.password=test1234
```

You can also pass this configuration via an environment variable. If you exporter ID is kafka, for
example, you could set the following environment variable:

```shell
export ZEEBE_BROKER_EXPORTERS_KAFKA_ARGS_PRODUCER_CONFIG="security.protocol=SSL\nssl.truststore.location=/truststore.jks\nssl.truststore.password=test1234"
```

## Examples

The [zeebe-kafka-exporter-qa](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/qa) module
shows how to start a Docker container, inject the exporter, configure it, and consume the exported records.

For a more normal deployment, you can look at the
[docker-compose.yml](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/docker-compose.yml)
file, which will start a Zeebe broker with the exporter configured via
[exporter.yml](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/exporter/exporter.yml),
a Zookeeper node, a Kafka node, and a consumer node which simply prints out everything send to Kafka
on any topic starting with `zeebe`.

### Consuming Zeebe records

As mentioned, Zeebe records are serialized using JSON. The key is the JSON representation of the
Java class
[RecordId](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/serde/src/main/java/io/zeebe/exporters/kafka/serde/RecordId.java),
and the value is serialized using the Zeebe `io.zeebe.protocol.record.Record#toJson()` method.

If you want to consume records via the Java client, you can make use of the deserializers provided
by the `io.zeebe.exporters:zeebe-kafka-exporter-serde` module:
    - [RecordIdDeserializer](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/serde/src/main/java/io/zeebe/exporters/kafka/serde/RecordIdDeserializer.java)
    - [RecordDeserializer](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/serde/src/main/java/io/zeebe/exporters/kafka/serde/RecordDeserializer.java)

An example of a consumer reading from all `zeebe-*` prefixed topics:

```java
package com.acme;

import io.zeebe.protocol.record.Record;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MyClass {
  private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
  private static final Pattern SUBSCRIPTION_PATTERN = Pattern.compile("^zeebe-.*$");

  public static void main(final String[] args) {
    final Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
    config.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.MAX_VALUE);
    config.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 5_000);

    final Consumer<RecordId, Record<?>> consumer =
      new KafkaConsumer<>(config, new RecordIdDeserializer(), new RecordDeserializer());
    consumer.subscribe(SUBSCRIPTION_PATTERN);

    while (true) {
      final ConsumerRecords<RecordId, Record<?>> consumed = consumer.poll(Duration.ofSeconds(1));
      for (final ConsumerRecord<RecordId, Record<?>> record : consumed) {
        LOGGER.info(
          "================[{}] {}-{} ================",
          record.topic(),
          record.key().getPartitionId(),
          record.key().getPosition());
        LOGGER.info("{}", record.value().getValue());
      }
    }
  }
}
```

## Docker

The
[docker-compose.yml](https://github.com/zeebe-io/zeebe-kafka-exporter/tree/master/docker-compose.yml)
found in the root of the project is a good example of how you can deploy Zeebe, Kafka, and connect
them via the exporter.

To run it, first build the correct exporter artifact which `docker-compose` can find. From the root
of the project, run:

```shell
mvn install -DskipTests -Dexporter.finalName=zeebe-kafka-exporter
```

> It's important here to note that we set the artifact's final name - this allows us to use a fixed
> name in the `docker-compose.yml` in order to mount the file to the Zeebe container.

Then you start the services - they can be started in parallel with no worries.

```shell
docker-compose up -d
```

> If you wish to stop these containers, remember that some of them create volumes, so unless you
> plan on reusing those make sure to bring everything down using `docker-compose down -v`.

The services started are the following:

  - zeebe: with the gateway port (26500) opened
  - kafka: with the standard 9092 port opened for internal communication, and port 29092 for external
  - consumer: a simple kafkacat image which will print out every record published on any topic starting with `zeebe`
  - zookeeper: required to start Kafka

Once everything is up and running, use your Zeebe cluster as you normally would. For example, given
a workflow at `~/workflow.bpmn`, you could deploy it as:

```shell
zbctl --insecure deploy ~/workflow.bpmn
```

After this, you can see the messages being consumed by the consumer running:

```shell
docker logs -f consumer
```

> You may see some initial error logs from the consumer - this happens while the Kafka broker isn't
> fully up, but it should stop once kafkacat can connect to it.

> The first time a record of a certain kind (e.g. deployment, job, workflow, etc.) is published, it
> will create a new topic for it. The consumer is refreshing the list of topics every second, which
> means that for that first message there may be a bit of delay.

## Reference

The exporter uses a Kafka producer to push records out to different topics based on the incoming record value type (e.g. deployment, etc.)

The producer is configured to be an idempotent producer which will retry a record "forever"; there is a delivery timeout configured, but the timeout is set
to ~25 days, which for most use cases should be enough to fix any recoverable errors. In the case of unrecoverable errors, unfortunately a restart is pretty much
the only solution at the moment, although community contributions are very welcome to fix this.

The main reason records are retried forever is that Zeebe processes records sequentially, and to ensure we've exported a record, we can't update Zeebe and say record 2
has been exported if we can't guarantee that previous records have also been exported (or in Kafka terms, acknowledged).

To take advantage of the asynchronous API and minimize blocking operations, the exporter keeps a queue of in-flight record futures (configurable) and will
export records until that queue is full; once full, it will block until the first element (i.e. the oldest sent record) has been acknowledged by Kafka, at which point
it will then send the next record and resume operation.

At the same time, a background job is scheduled every second to flush the queue of any completed records. So in a best case scenario the queue always has some space and
the exporter never blocks.

## Configuration

A sample configuration file is included in the project under `exporter.yml`.

```yaml
zeebe:
  broker:
    exporters:
      kafka:
        className: io.zeebe.exporters.kafka.KafkaExporter
        args:
          # Controls how many records can have been sent to the Kafka broker without
          # any acknowledgment Once the limit is reached the exporter will block and
          # wait until either one record is acknowledged
          maxInFlightRecords: 1000
          # How often should the exporter drain the in flight records' queue of completed
          # requests and update the broker with the guaranteed latest exported position
          inFlightRecordCheckIntervalMs: 1000

          # Producer specific configuration
          producer:
            # The list of initial Kafka broker contact points. The format should be the same
            # one as the ProducerConfig expects, i.e. "host:port"
            # Maps to ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
            # For example:
            # servers:
            #   - kafka:9092
            servers: []
            # Controls how long the producer will wait for a request to be acknowledged by
            # the Kafka broker before retrying it
            # Maps to ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG
            requestTimeoutMs: 5000
            # Grace period when shutting down the producer in milliseconds
            closeTimeoutMs: 5000
            # Producer client identifier
            clientId: zeebe
            # Max concurrent requests to the Kafka broker; note that in flight records are batched such that
            # in one request you can easily have a thousand records, depending on the producer's batch
            # configuration.
            maxConcurrentRequests: 3

            # Any setting under the following section will be passed verbatim to
            # ProducerConfig; you can use this to configure authentication, compression,
            # etc. Note that you can overwrite some important settings, so avoid changing
            # idempotency, delivery timeout, and retries, unless you know what you're doing
            config: |
              linger.ms=5
              buffer.memory=8388608
              batch.size=32768
              max.block.ms=5000

          # Controls which records are pushed to Kafka and to which topic
          # Each entry is a sub-map which can contain two entries:
          #     type => string
          #     topic => string
          #
          # Topic is the topic to which the record with the given value type
          # should be sent to, e.g. for a deployment record below we would
          # send the record to "zeebe-deployment" topic.
          #
          # Type is a comma separated string of accepted record types, allowing you to filter if you
          # want nothing (""), commands ("command"), events ("events"), or rejections ("rejection"),
          # or a combination of the three, e.g. "command,event".
          #
          # To omit certain records entirely, set type to an empty string. For example,
          # records:
          #   deployment: { type: "" }
          records:
            # If a record value type is omitted in your configuration file,
            # it will fall back to whatever is configured in the defaults
            defaults: { type: "event", topic: zeebe }
            # For records with a value of type DEPLOYMENT
            deployment: { topic: zeebe-deployment }
            # For records with a value of type INCIDENT
            incident = { topic: zeebe-incident }
            # For records with a value of type JOB_BATCH
            jobBatch: { topic = "zeebe-job-batch" }
            # For records with a value of type JOB
            job: { topic: zeebe-job }
            # For records with a value of type MESSAGE
            message: { topic: zeebe-message }
            # For records with a value of type MESSAGE_SUBSCRIPTION
            messageSubscription: { topic: zeebe-message-subscription }
            # For records with a value of type MESSAGE_START_EVENT_SUBSCRIPTION
            messageStartEventSubscription: { topic: zeebe-message-subscription-start-event }
            # For records with a value of type TIMER
            timer: { topic: zeebe-timer }
            # For records with a value of type VARIABLE
            variable: { topic: zeebe-variable }
            # For records with a value of type WORKFLOW_INSTANCE
            workflowInstance: { topic: zeebe-workflow }
            # For records with a value of type WORKFLOW_INSTANCE_RESULT
            workflowInstanceResult: { topic: zeebe-workflow-result }
            # For records with a value of type WORKFLOW_INSTANCE_SUBSCRIPTION
            workflowInstanceSubscription: { topic: zeebe-workflow-subscription }
```
