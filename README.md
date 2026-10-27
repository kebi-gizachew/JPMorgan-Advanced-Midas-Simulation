# Midas
A cutting-edge JPMorgan simulation project that replicates real-world financial transaction workflows using Java, Spring Boot, and Kafka, providing hands-on experience with enterprise-level software engineering.

## Transaction audit service

The separately runnable `transaction-audit-service/` application observes the existing
`trader-updates` Kafka topic using the same JSON transaction fields (`senderId`,
`recipientId`, and `amount`) and a different consumer group (`midas-audit-group`).
It logs each observed event and keeps a demonstration count in memory. The event has
no unique transaction ID, so the sender/recipient pair and amount are logged instead.

The counter is not durable and resets when the audit service restarts. Kafka does not
provide exactly-once processing across this service and the MIDAS consumer: restarts,
rebalances, or retries may result in duplicate observations. Invalid records and
listener failures are logged and skipped without retry by the audit service's error
handler. The audit service does not update MIDAS balances or call the incentive API.

### Build and run

Use Java 17 or newer. These commands use the original project's Maven wrapper from the
repository root. Kafka must already be reachable at `localhost:9092`, unless the
bootstrap server is overridden.

Build the original application:

```powershell
.\mvnw.cmd -DskipTests package
```

Run the original application in one terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

Build and run the audit service in another terminal:

```powershell
.\mvnw.cmd -f transaction-audit-service\pom.xml package
java -jar transaction-audit-service\target\transaction-audit-service-1.0.0.jar
```

The audit service accepts these optional environment variables:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:KAFKA_TOPIC = "trader-updates"
$env:AUDIT_KAFKA_GROUP_ID = "midas-audit-group"
```

Note: the existing `KafkaProducer` helper is under `src/test`, so it is available to
the guided tests but is not part of the runnable MIDAS application. A live demonstration
requires a producer publishing the existing JSON transaction format to the configured
topic. The audit service integration test uses an embedded test broker and publishes a
compatible sample event.
