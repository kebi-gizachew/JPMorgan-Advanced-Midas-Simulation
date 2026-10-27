package com.jpmc.midas.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.kafka.support.serializer.JsonSerializer;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = "trader-updates",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class TransactionAuditKafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private TransactionAuditListener listener;

    @Value("${general.kafka-topic}")
    private String topic;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Test
    void skipsMalformedJsonAndConsumesTransactionJsonInASeparateGroup() throws Exception {
        assertEquals("trader-updates", topic);
        assertNotEquals("midas-core-group", consumerGroup);

        var producerProperties = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        try (var invalidProducer = new org.apache.kafka.clients.producer.KafkaProducer<>(
                producerProperties, new StringSerializer(), new StringSerializer())) {
            invalidProducer.send(new ProducerRecord<>(topic, "{invalid-json"))
                    .get(10, TimeUnit.SECONDS);
        }

        try (var producer = new org.apache.kafka.clients.producer.KafkaProducer<>(
                producerProperties, new StringSerializer(), new JsonSerializer<TransactionEvent>())) {
            producer.send(new ProducerRecord<>(topic, new TransactionEvent(4L, 9L, 13.75f)))
                    .get(10, TimeUnit.SECONDS);
        }

        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (listener.getObservedEventCount() == 0L && System.nanoTime() < deadline) {
            Thread.sleep(25L);
        }

        assertEquals(1L, listener.getObservedEventCount());
    }
}
