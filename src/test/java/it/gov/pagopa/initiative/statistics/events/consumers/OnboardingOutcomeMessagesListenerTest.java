package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.onboarding.OnboardingStatisticsService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.Mockito.*;

class OnboardingOutcomeMessagesListenerTest {

    private OnboardingStatisticsService onboardingStatisticsService;
    private OnboardingOutcomeMessagesListener listener;

    @BeforeEach
    void setUp() {
        onboardingStatisticsService = mock(OnboardingStatisticsService.class);
        listener = new OnboardingOutcomeMessagesListener(onboardingStatisticsService);
    }

    @Test
    void shouldDelegateRecordsToOnboardingStatisticsService() {
        // Arrange
        ConsumerRecord<String, String> consumerRecord =
                new ConsumerRecord<>("onboarding-topic", 0, 0L, "key", "value");
        List<ConsumerRecord<String, String>> records = List.of(consumerRecord);

        @SuppressWarnings("unchecked")
        Consumer<String, String> consumer = mock(Consumer.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        // Act
        listener.onMessage(records, acknowledgment, consumer);

        // Assert
        verify(onboardingStatisticsService, times(1))
                .evaluate(records, consumer);

        // Verifica che non venga effettuato l'ack automatico
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void shouldHandleEmptyRecords() {
        // Arrange
        List<ConsumerRecord<String, String>> records = List.of();

        @SuppressWarnings("unchecked")
        Consumer<String, String> consumer = mock(Consumer.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        // Act
        listener.onMessage(records, acknowledgment, consumer);

        // Assert
        verify(onboardingStatisticsService).evaluate(records, consumer);
        verifyNoInteractions(acknowledgment);
    }
}