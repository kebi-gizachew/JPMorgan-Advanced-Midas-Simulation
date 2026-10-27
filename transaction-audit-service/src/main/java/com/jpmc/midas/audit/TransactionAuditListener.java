package com.jpmc.midas.audit;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionAuditListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionAuditListener.class);

    private final AtomicLong observedEventCount = new AtomicLong();

    @KafkaListener(
            topics = "${general.kafka-topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listen(TransactionEvent transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction event must not be null");
        }

        long count = observedEventCount.incrementAndGet();
        log.info("Observed transaction event #{}: senderId={}, recipientId={}, amount={}",
                count, transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());
    }

    public long getObservedEventCount() {
        return observedEventCount.get();
    }
}
