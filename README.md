## kafka-connect-druid

*WORK IN PROGRESS -- TREAD CAREFULLY*

### Building

```bash
sbt ++2.11.8 assembly
```

### Quickstart

Installation:

```bash
cd mytestdir
curl -O http://static.imply.io/release/imply-1.2.0.tar.gz
curl -O http://apache.osuosl.org/kafka/0.9.0.1/kafka_2.11-0.9.0.1.tgz
tar -xzf imply-1.2.0.tar.gz
tar -xzf kafka_2.11-0.9.0.1.tgz
cp /path/to/kafka-connect-druid/target/scala-2.11/kafka-connect-druid-assembly-0.1.0-SNAPSHOT.jar kafka_2.11-0.9.0.1/libs/
```

You might have to remove kafka_2.11-0.9.0.1/libs/jackson-* too?

Configuration:

- druid.specString should be a json encoded ingestion spec
- all other properties as described on https://github.com/druid-io/tranquility/blob/master/docs/configuration.md#properties
