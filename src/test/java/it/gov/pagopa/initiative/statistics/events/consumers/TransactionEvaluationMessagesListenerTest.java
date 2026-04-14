package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.trx.TransactionEvaluationStatisticsService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEvaluationMessagesListenerTest {

    @Mock
    private TransactionEvaluationStatisticsService transactionEvaluationStatisticsService;

    @Mock
    private Consumer<String, String> consumer;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldDelegateRecordsToTransactionEvaluationStatisticsService() {
        // Arrange
        TransactionEvaluationMessagesListener listener =
                new TransactionEvaluationMessagesListener(transactionEvaluationStatisticsService);

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("transaction-evaluation-topic", 0, 0L, "userId", "{json}");

        List<ConsumerRecord<String, String>> records = List.of(record);

        // Act
        listener.onMessage(records, acknowledgment, consumer);

        // Assert
        verify(transactionEvaluationStatisticsService, times(1))
                .evaluate(records, consumer);

        // L'acknowledgment non deve essere utilizzato
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void shouldDelegateEvenWhenRecordsAreEmpty() {
        // Arrange
        TransactionEvaluationMessagesListener listener =
                new TransactionEvaluationMessagesListener(transactionEvaluationStatisticsService);

        List<ConsumerRecord<String, String>> records = List.of();

        // Act
        listener.onMessage(records, acknowledgment, consumer);

        // Assert
        verify(transactionEvaluationStatisticsService)
                .evaluate(records, consumer);

        verifyNoInteractions(acknowledgment);
    }
}