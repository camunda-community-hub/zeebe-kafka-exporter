Zeebe Kafka Exporter
=====================

An easy to use exporter which will export Zeebe records to a configured Kafka topic.

If you have any problems please open an issue, write in the forum or on slack.

For more information about the exporters please read the [Exporter documentation](https://docs.zeebe.io/basics/exporters.html).

> This is a work in progress; you're welcome to contribute code or ideas, but no guarantees are made about the exporter itself. Use at your own risks.

> There is a docker compose and some docker files around, in theory these work, but doing a `docker-compose up` is currently broken until the next Zeebe release,
> since it relies on Zeebe 0.15.0 which is not yet released. But doing a `docker-compose up kafka zookeeper consumer` should work, and you'd need to configure the broker
> manually to point to Kafka on port 9093.

## Installation

The quickest way to get started is:

1. Download the latest release (`zeebe-kafka-exporter-*-uber.jar`).
1. Copy it to the `lib/` folder of your Zeebe brokers.
1. Copy the configuration from `exporter.kafka.cfg.toml` into each broker's `zeebe.cfg.toml` (e.g. append it at the end).
1. Update the configuration's list of servers to point to your Kafka instances.

The next time you start your Zeebe cluster, all event-type records will be exported to their respective Kafka topics.

## Consuming

> NOTE: it's planned to offer baked-in `Deserializer` to ease writing consumers, but I'm not sure what's the best approach yet. One attempt is using Flatbuffers to offer fast, consistent, language agnostic serialization/deserialization, but it's not yet sure.

The exporter serializes records to their respective topics using the following format:

- **key**: `string` => <partitionId>-<position> e.g. 0-8589984192, 1-8589979304
- **value**: `string` => the record JSON representation as obtained through `io.zeebe.exporter.record.Record#toJson()`

Basic deserializer examples are included in the samples project.

The topic to which a record is pushed to is configured under the `[exporter.args.records]` section.

## Reference

The exporter uses a Kafka producer to push records out to different topics based on the incoming record value type (e.g. deployment, raft, etc.)

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

A sample configuration file is included in the project under `exporter.kafka.cfg.toml`.

```toml
[[exporters]]
id = "kafka"
className = "io.zeebe.exporter.kafka.KafkaExporter"

  # Top level exporter arguments
  [exporters.args]
  # Controls how many records can have been sent to the Kafka broker without
  # any acknowledgment Once the limit is reached the exporter will block and
  # wait until either one record is acknowledged or awaitInFlightRecordTimeout
  # time is elapsed, at which point an error is produced and the record is
  # retried at a later time
  maxInFlightRecords = 1000
  # Controls how long we should block and await a record to be acknowledged
  # by the Kafka broker when the maximum number of in-flight records is reached
  # Format is the same as standard Zeebe configuration timeouts/durations
  awaitInFlightRecordTimeout = "5s"

  # Producer specific configuration
  [exporters.args.producer]
  # The list of initial Kafka broker contact points. The format should be the same
  # one as the ProducerConfig expects, i.e. "host:port"
  # Maps to ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
  servers = [ "kafka:9092" ]
  # Controls how long the producer will wait for a request to be acknowledged by
  # the Kafka broker before retrying it
  # Maps to ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG
  # Format is the same as standard Zeebe configuration timeouts/durations
  requestTimeout = "5s"

  # Any setting under the following section will be passed verbatim to
  # ProducerConfig; you can use this to configure authentication, compression,
  # etc. Note that you can overwrite some important settings, so avoid changing
  # idempotency, delivery timeout, and retries, unless you know what you're doing
  [exporters.args.producer.config]
  "batch.size" = 131072
  "linger.ms" = 50

  # Controls which records are pushed to Kafka and to which topic
  # Each entry is a sub-map which can contain two entries:
  #     type => [string]
  #     topic => string
  #
  # Topic is the topic to which the record with the given value type
  # should be sent to, e.g. for a deployment record below we would
  # send the record to "zeebe-deployment" topic.
  #
  # Type is a list of accepted record types, allowing you to filter
  # if you want nothing ([]), commands (["command"]), events (["events"]),
  # or rejections (["rejection"]), or a combination of the three, e.g.
  # ["command", "event"].
  [exporters.args.records]
  # If a record value type is omitted in your configuration file,
  # it will fall back to whatever is configured in the defaults
  defaults = { type = [ "event" ], topic = "zeebe" }
  # For records with a value of type DEPLOYMENT
  deployment = { topic = "zeebe-deployment" }
  # For records with a value of type INCIDENT
  incident = { topic = "zeebe-incident" }
  # For records with a value of type JOB_BATCH
  jobBatch = { topic = "zeebe-job-batch" }
  # For records with a value of type JOB
  job = { topic = "zeebe-job" }
  # For records with a value of type MESSAGE
  message = { topic = "zeebe-message" }
  # For records with a value of type MESSAGE_SUBSCRIPTION
  messageSubscription = { topic = "zeebe-message-subscription" }
  # For records with a value of type MESSAGE_START_EVENT_SUBSCRIPTION
  messageStartEventSubscription = { topic = "zeebe-message-subscription-start-event" }
  # For records with a value of type RAFT
  raft = { topic = "zeebe-raft" }
  # For records with a value of type TIMER
  timer = { topic = "zeebe-timer" }
  # For records with a value of type WORKFLOW_INSTANCE
  workflowInstance = { topic = "zeebe-workflow" }
  # For records with a value of type WORKFLOW_INSTANCE_SUBSCRIPTION
  workflowInstanceSubscription = { topic = "zeebe-workflow-subscription" }
```
