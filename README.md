Zeebe Kafka Exporter
=====================

[![](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)
[![](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)
[![](https://img.shields.io/github/v/release/camunda-community-hub/zeebe-kafka-exporter?sort=semver)](https://github.com/camunda-community-hub/zeebe-kafka-exporter/releases/latest)
[![Java CI](https://github.com/camunda-communit-hub/zeebe-kafka-exporter/actions/workflows/ci.yml/badge.svg)](https://github.com/camunda-community-hub/zeebe-kafka-exporter/actions/workflows/ci.yml)

An easy to use exporter which will export Zeebe records to a configured Kafka topic.

> For more information about the exporters please read the
> [Exporter documentation](https://docs.zeebe.io/basics/exporters.html).

## Supported Zeebe versions

Version 1.x and 2.x is compatible with the following Zeebe versions:

- 0.23.x
- 0.24.x
- 0.25.x
- 0.26.x

Version 3.x is compatible with the following Zeebe versions:

- 1.0

## Backwards compatibility

As there is currently only a single maintainer, only the latest major version will be maintained and
supported.

At the moment, the only guarantees for backwards compatibility are:

- the exporter's configuration
- the serde module

## Quick start

The quickest way to get started is:

1. Download the latest release (`zeebe-kafka-exporter-*-jar-with-dependencies.jar`).
1. Copy it to the `lib/` folder of your Zeebe brokers.
1. Copy the contents of the configuration from `exporter.yml` into your Zeebe `application.yml`
1. Update the configuration's list of servers to point to your Kafka instances.

The next time you start your Zeebe cluster, all event-type records will be exported to their
respective Kafka topics.

> NOTE: there's a bug in Zeebe which prevents this exporter from being loaded in an isolated way.
> See [this issue](https://github.com/camunda-cloud/zeebe/issues/4196) for more. When that is fixed,
> then the exporter should be loaded as any other external exporter.

## Usage

The exporter is set up to stream records from Zeebe to Kafka as they are processed by the exporter
stream processor. While this is done asynchronously, it makes use
of [Kafka transactions](https://www.confluent.io/blog/transactions-apache-kafka/)
to minimize issues with out-of-order processing. As such, your consumers should use `read_committed`
isolation level.

Records are serialized to Kafka using plain JSON. Keys are JSON representation of
`io.zeebe.exporters.kafka.serde.RecordId`, and values are serialized using the standard Zeebe
`io.zeebe.protocol.record.Record#toJson()` method.
The `io.zeebe.exporters:zeebe-kafka-exporter-serde`
module provides easy to use `Deserializer` implementations in Java for use in your consumers.

The [configuration file](/exporter/exporter.yml) is a good starting point to learn more about how
the exporter works.

### Kafka configuration

You may need to configure your Kafka instance(s) for the exporter. It's recommended that you
provision the expected topics beforehand, so you can configure them properly beforehand. You can
read more below about partitioning.

#### Transactions

> NOTE: I'm still planning on adding an alternative method that doesn't require transactions, as
> we can't make use of exactly-once semantics anyway due to Zeebe's at-least once semantics.
> However, it needs to be fault tolerant as well, so it may take a bit more time.

Additionally, the exporter makes use of transactions, and it's recommended that you configure
transactions accordingly for your brokers. You can find a description of the relevant settings in
[the official Kafka documentation](https://kafka.apache.org/documentation/#brokerconfigs_transaction.max.timeout.ms).
The important settings are:

- `transaction.state.log.min.isr`: the minimum number of in-sync replicas for the transaction topic.
  By default it's 2, but if you're running a single node cluster (i.e. for demo or testing
  purposes, make sure to lower it to 1).
- `transaction.state.log.replication.factor`: by default 3, such that transactions can handle one
  failure. Again, if running a single node for demo/testing purposes, lower this to 1.
- `transaction.state.log.segment.bytes`: you can leave it as default, but it can be even smaller to
  more aggressively compact, considering that the exporter flushes fairly often.
- `transaction.id.expiration.ms`: configure this with respect to the exporter's flush interval, and
  how much load your cluster will see. By default, the exporter flushes every second - however, on
  a low load cluster, there may not be anything to flush at times. It's recommended to set this low
  if you have constant load - say, one hour - but keep it to the default if your load is irregular.
- `transaction.max.timeout.ms`: configure this with respect to the exporter's flush interval, and
  how much load your cluster will see. By default, the exporter flushes every second - however, on
  a low load cluster, there may not be anything to flush at times. It's recommended to set this low
  if you have constant load - say, one hour - but keep it to the default if your load is irregular.

#### Partitioning

As ordering in Zeebe is critical to understanding the flow of events, it's important that it be
preserved in Kafka as well. To achieve this, the exporter implements its own `Partitioner`.

It does so by taking the Zeebe partition ID (which starts at 1), and applying a modulo against the
number of Kafka partitions for the given topic, e.g. `zeebePartitionId % kafkaPartitionsCount`.

> One downside is that if you have more Kafka partitions than Zeebe partitions, some of your
> partitions will be unused: partition 0, and any partition whose number is greater than the count
> of Zeebe partitions. **As such, it's completely useless to add more Kafka partitions than Zeebe
> partitions in most cases.**

For example, if you have 3 Zeebe partitions, and 2 Kafka partitions:

- `RecordId{partitionId=1, position=1}` => Kafka partition 1
- `RecordId{partitionId=2, position=1}` => Kafka partition 0
- `RecordId{partitionId=3, position=1}` => Kafka partition 1
- `RecordId{partitionId=3, position=2}` => Kafka partition 1
- `RecordId{partitionId=2, position=2}` => Kafka partition 0

With more Kafka partitions, for example, 4 Kafka partitions, and 3 Zeebe partitions:

- `RecordId{partitionId=1, position=1}` => Kafka partition 1
- `RecordId{partitionId=2, position=1}` => Kafka partition 2
- `RecordId{partitionId=3, position=1}` => Kafka partition 3
- `RecordId{partitionId=3, position=2}` => Kafka partition 3
- `RecordId{partitionId=2, position=2}` => Kafka partition 2

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

The [zeebe-kafka-exporter-qa](/qa)
module shows how to start a Docker container, inject the exporter, configure it, and consume the
exported records.

For a more normal deployment, you can look at the [docker-compose.yml](/docker-compose.yml) file,
which will start a Zeebe broker with the exporter configured via [exporter.yml](/exporter/exporter.yml),
a Zookeeper node, a Kafka node, and a consumer node which simply prints out everything send to Kafka
on any topic starting with `zeebe`.

### Consuming Zeebe records

As mentioned, Zeebe records are serialized using JSON. The key is the JSON representation of the
Java class [RecordId](/serde/src/main/java/io/zeebe/exporters/kafka/serde/RecordId.java), and the
value is serialized using the Zeebe `io.zeebe.protocol.record.Record#toJson()` method.

If you want to consume records via the Java client, you can make use of the deserializers provided
by the `io.zeebe.exporters:zeebe-kafka-exporter-serde` module:

- [RecordIdDeserializer](/serde/src/main/java/io/zeebe/exporters/kafka/serde/RecordIdDeserializer.java)
- [RecordDeserializer](/serde/src/main/java/io/zeebe/exporters/kafka/serde/RecordDeserializer.java)

An example of a consumer reading from all `zeebe-*` prefixed topics:

```java
package com.acme;

import io.camunda.zeebe.protocol.record.Record;
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
    config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

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

### Docker

The [docker-compose.yml](/docker-compose.yml) found in the root of the project is a good example of
how you can deploy Zeebe, Kafka, and connect them via the exporter.

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
- consumer: a simple kafkacat image which will print out every record published on any topic
  starting with `zeebe`
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

The exporter uses a Kafka producer to push records out to different topics based on the incoming
record value type (e.g. deployment, etc.).

The producer makes use of transactions to simplify

The producer is configured to be an idempotent producer which will retry a record "forever"; there
is a delivery timeout configured, but the timeout is set to ~25 days, which for most use cases
should be enough to fix any recoverable errors. In the case of unrecoverable errors, unfortunately a
restart is pretty much the only solution at the moment, although community contributions are very
welcome to fix this.

The main reason records are retried forever is that Zeebe processes records sequentially, and to
ensure we've exported a record, we can't update Zeebe and say record 2 has been exported if we can't
guarantee that previous records have also been exported (or in Kafka terms, acknowledged).

To take advantage of the asynchronous API and minimize blocking operations, the exporter keeps a
queue of in-flight record futures (configurable) and will export records until that queue is full;
once full, it will block until the first element (i.e. the oldest sent record) has been acknowledged
by Kafka, at which point it will then send the next record and resume operation.

At the same time, a background job is scheduled every second to flush the queue of any completed
records. So in a best case scenario the queue always has some space and the exporter never blocks.

## Configuration

A sample configuration file is included in the project under `exporter.yml`.

```yaml
zeebe:
  broker:
    exporters:
      kafka:
        className: io.zeebe.exporters.kafka.KafkaExporter
        args:
          # Controls the number of records to buffer in a single record batch before forcing a flush. Note
          # that a flush may occur before anyway due to periodic flushing. This setting should help you
          # estimate a soft upper bound to the memory consumption of the exporter. If you assume a worst
          # case scenario where every record is the size of your zeebe.broker.network.maxMessageSize, then
          # the memory required by the exporter would be at least:
          #   (maxBatchSize * zeebe.broker.network.maxMessageSize * 2)
          #
          # We multiply by 2 as the records are buffered twice - once in the exporter itself, and once
          # in the producer's network buffers (but serialized at that point). There's some additional
          # memory overhead used by the producer as well for compression/encryption/etc., so you have to
          # add a bit, but that one is not proportional to the number of records and is more or less
          # constant.
          #
          # Once the batch has reached this size, a flush is automatically triggered. Too small a number
          # here would cause many flush, which is not good for performance, but would mean you will see
          # your records faster/sooner.
          #
          # Default is 100
          maxBatchSize: 100
          # The maximum time to block when the batch is full. If the batch is full, and a new
          # record comes in, the exporter will block until there is space in the batch, or until
          # maxBlockingTimeoutMs milliseconds elapse.
          maxBlockingTimeoutMs: 1000
          # How often should pending batches be flushed to the Kafka broker. Too low a value will
          # cause more load on the broker, but means your records will be visible faster.
          flushIntervalMs: 1000

          # Producer specific configuration
          producer:
            # The list of initial Kafka broker contact points. The format should be the same
            # one as the ProducerConfig expects, i.e. "host:port"
            # Maps to ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
            # For example:
            # servers: "kafka:9092,localhost:29092"
            servers: ""
            # Controls how long the producer will wait for a request to be acknowledged by
            # the Kafka broker before retrying it
            # Maps to ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG
            requestTimeoutMs: 5000
            # Grace period when shutting down the producer in milliseconds
            closeTimeoutMs: 5000
            # Producer client identifier
            clientId: zeebe

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
            # For records with a value of type DEPLOYMENT_DISTRIBUTION
            deploymentDistribution: { topic: zeebe-deployment-distribution }
            # For records with a value of type ERROR
            error: { topic: zeebe-error }
            # For records with a value of type INCIDENT
            incident: { topic: zeebe-incident }
            # For records with a value of type JOB_BATCH
            jobBatch: { topic: zeebe-job-batch }
            # For records with a value of type JOB
            job: { topic: zeebe-job }
            # For records with a value of type MESSAGE
            message: { topic: zeebe-message }
            # For records with a value of type MESSAGE_SUBSCRIPTION
            messageSubscription: { topic: zeebe-message-subscription }
            # For records with a value of type MESSAGE_START_EVENT_SUBSCRIPTION
            messageStartEventSubscription: { topic: zeebe-message-subscription-start-event }
            # For records with a value of type PROCESS
            process: { topic: zeebe-process }
            # For records with a value of type PROCESS_EVENT
            processEvent: { topic: zeebe-process-event }
            # For records with a value of type PROCESS_INSTANCE
            processInstance: { topic: zeebe-process-instance }
            # For records with a value of type PROCESS_INSTANCE_RESULT
            processInstanceResult: { topic: zeebe-process-instance-result }
            # For records with a value of type PROCESS_MESSAGE_SUBSCRIPTION
            processMessageSubscription: { topic: zeebe-process-message-subscription }
            # For records with a value of type TIMER
            timer: { topic: zeebe-timer }
            # For records with a value of type VARIABLE
            variable: { topic: zeebe-variable }
```

# Contributing

Contributions are more than welcome! Please make sure to read and adhere to the
[Code of Conduct](CODE_OF_CONDUCT.md). Additionally, in order to have your contributions accepted,
you will need to sign the [Contributor License Agreement](https://cla-assistant.io/camunda/).

## Build from source

### Prerequisites

In order to build from source, you will need to install maven 3.6+. You can find more about it on
the [maven homepage](https://maven.apache.org/users/index.html).

You will also need a JDK targeting Java 8+. We recommend installing any flavour of OpenJDK such as
[AdoptOpenJDK](https://adoptopenjdk.net/).

Finally, you will need to [install Docker](https://docs.docker.com/get-docker/) on your local
machine, as integration tests rely heavily on [Testcontainers](https://testcontainers.org).

### Building

With all requirements ready, you can now simply
[clone the repository](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository),
and from its root, run the following command:

```shell
mvn clean install
```

This will build the project and run all tests locally.

Should you wish to only build without running the tests, you can run:

```shell
mvn clean package
```

## Backwards compatibility

Zeebe Kafka Exporter uses a [Semantic Versioning](https://semver.org/) scheme for its versions, and
[revapi](https://revapi.org/) to enforce backwards compatibility according to its specification.

Additionally, we also use [apiguardian](https://github.com/apiguardian-team/apiguardian) to specify
backwards compatibility guarantees on a more granular level. As such, any APIs marked as
`EXPERIMENTAL` will not be checked.

If you wish to incubate a new feature, or if you're unsure about a new API type/method, please use
the `EXPERIMENTAL` status for it. This will give us flexibility to test out new features and change
them easily if we realize they need to be adapted.

## Report issues or contact developers

Work on Zeebe Kafka Exporter is done entirely through the Github repository. If you want to report a
bug or request a new feature feel free to open a new issue on [GitHub][issues].

## Create a Pull Request

To work on an issue, follow the following steps:

1. Check that a [GitHub issue][issues] exists for the task you want to work on. If one does not,
   create one.
1. Checkout the `master` branch and pull the latest changes.
   ```
   git checkout develop
   git pull
   ```
1. Create a new branch with the naming scheme `issueId-description`.
   ```
   git checkout -b 123-my-new-feature
   ```
1. Follow
   the [Google Java Format](https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides)
   and [Zeebe Code Style](https://github.com/zeebe-io/zeebe/wiki/Code-Style) while coding.
1. Implement the required changes on your branch, and make sure to build and test your changes
   locally before opening a pull requests for review.
1. If you want to make use of the CI facilities before your feature is ready for review, feel free
   to open a draft PR.
1. If you think you finished the issue please prepare the branch for reviewing. In general the
   commits should be squashed into meaningful commits with a helpful message. This means cleanup/fix
   etc commits should be squashed into the related commit.
1. Finally, be sure to check on the CI results and fix any reported errors.

## Commit Message Guidelines

Commit messages use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary)
format, with a slight twist. See the
[Zeebe commit guidelines for more](https://github.com/camunda-cloud/zeebe/blob/develop/CONTRIBUTING.md#commit-message-guidelines)
.

## Contributor License Agreement

You will be asked to sign our Contributor License Agreement when you open a Pull Request. We are not
asking you to assign copyright to us, but to give us the right to distribute your code without
restriction. We ask this of all contributors in order to assure our users of the origin and
continuing existence of the code. You only need to sign the CLA once.

Note that this is a general requirement of any Camunda Community Hub project.
