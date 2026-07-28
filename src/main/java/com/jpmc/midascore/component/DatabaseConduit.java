package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseConduit {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConduit.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final IncentiveApiClient incentiveApiClient;

    public DatabaseConduit(UserRepository userRepository, TransactionRepository transactionRepository, IncentiveApiClient incentiveApiClient) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.incentiveApiClient = incentiveApiClient;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    @Transactional
    public void processTransaction(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null || recipient == null) {
            log.debug("Discarded transaction: sender or recipient not found (senderId={}, recipientId={}, amount={})",
                    transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());
            return;
        }

        log.debug("Received transaction: {} -> {}, amount={}",
                sender.getName(), recipient.getName(), transaction.getAmount());

        if (sender.getBalance() < transaction.getAmount()) {
            log.debug("Discarded transaction: {} -> {}, amount={} (insufficient balance: {})",
                    sender.getName(), recipient.getName(), transaction.getAmount(), sender.getBalance());
            return;
        }

        Incentive incentive = incentiveApiClient.query(transaction);
        float incentiveAmount = incentive.getAmount();

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        userRepository.save(sender);
        userRepository.save(recipient);
        transactionRepository.save(new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount));

        log.debug("Recorded transaction: {} -> {}, amount={}",
                sender.getName(), recipient.getName(), transaction.getAmount());
    }

}
