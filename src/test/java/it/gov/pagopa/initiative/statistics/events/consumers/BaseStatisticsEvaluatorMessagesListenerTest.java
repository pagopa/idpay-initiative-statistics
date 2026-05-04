package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.Mockito.*;

class BaseStatisticsEvaluatorMessagesListenerTest {

    @Test
    void shouldDelegateToStatisticsEvaluationService() {

        // Arrange
        StatisticsEvaluationService service = mock(StatisticsEvaluationService.class);

        BaseStatisticsEvaluatorMessagesListener listener =
                new BaseStatisticsEvaluatorMessagesListener(service) {};

        ConsumerRecord<String, String> consumerRecord =
                new ConsumerRecord<>("topic", 0, 0L, "key", "value");

        List<ConsumerRecord<String, String>> records = List.of(consumerRecord);

        Consumer<String, String> consumer = mock(Consumer.class);
        Acknowledgment ack = mock(Acknowledgment.class);

        // Act
        listener.onMessage(records, ack, consumer);

        // Assert
        verify(service, times(1)).evaluate(records, consumer);
        verifyNoInteractions(ack);
    }
}