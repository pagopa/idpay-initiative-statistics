package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.merchant.counters.notification.MerchantNotificationStatisticsService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantCountersRewardMessagesListenerTest {

    @Mock
    private MerchantNotificationStatisticsService merchantNotificationStatisticsService;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private Consumer<?, ?> consumer;

    @InjectMocks
    private MerchantCountersRewardMessagesListener listener;

    @Test
    void shouldDelegateToStatisticsService() {
        // Arrange
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("merchant-counters-reward-notification", 0, 0L, "key", "value");
        List<ConsumerRecord<String, String>> records = List.of(record);

        // Act
        listener.onMessage(records, acknowledgment, consumer);

        // Assert
        verify(merchantNotificationStatisticsService, times(1))
                .evaluate(records, consumer);
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void shouldHandleEmptyRecords() {
        // Arrange
        List<ConsumerRecord<String, String>> records = List.of();

        // Act
        listener.onMessage(records, acknowledgment, consumer);

        // Assert
        verify(merchantNotificationStatisticsService)
                .evaluate(records, consumer);
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void shouldPropagateExceptionThrownByService() {
        // Arrange
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("merchant-counters-reward-notification", 0, 0L, "key", "value");
        List<ConsumerRecord<String, String>> records = List.of(record);

        doThrow(new RuntimeException("Test exception"))
                .when(merchantNotificationStatisticsService)
                .evaluate(records, consumer);

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> listener.onMessage(records, acknowledgment, consumer));

        verify(merchantNotificationStatisticsService)
                .evaluate(records, consumer);
        verifyNoInteractions(acknowledgment);
    }
}