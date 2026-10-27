package com.jpmc.midas.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class TransactionAuditListenerTest {

    private final TransactionAuditListener listener = new TransactionAuditListener();

    @Test
    void logsTransactionFieldsAndIncrementsObservedCount(CapturedOutput output) {
        listener.listen(new TransactionEvent(7L, 12L, 25.5f));

        assertEquals(1L, listener.getObservedEventCount());
        assertTrue(output.getOut().contains("senderId=7"));
        assertTrue(output.getOut().contains("recipientId=12"));
        assertTrue(output.getOut().contains("amount=25.5"));
    }

    @Test
    void rejectsNullEventWithoutIncrementingCount() {
        assertThrows(IllegalArgumentException.class, () -> listener.listen(null));
        assertEquals(0L, listener.getObservedEventCount());
    }
}
