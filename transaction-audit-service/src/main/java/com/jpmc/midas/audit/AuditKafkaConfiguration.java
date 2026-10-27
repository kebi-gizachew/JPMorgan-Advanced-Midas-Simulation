package com.jpmc.midas.audit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class AuditKafkaConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaConfiguration.class);

    @Bean
    public CommonErrorHandler auditKafkaErrorHandler() {
        return new DefaultErrorHandler(this::logSkippedRecord, new FixedBackOff(0L, 0L));
    }

    private void logSkippedRecord(ConsumerRecord<?, ?> record, Exception exception) {
        log.error("Skipping invalid transaction event from topic {} at offset {}",
                record.topic(), record.offset(), exception);
    }
}
