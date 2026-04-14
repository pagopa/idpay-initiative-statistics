package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.commands.CommandsMediatorService;
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
class CommandsMessagesListenerTest {

    @Mock
    private CommandsMediatorService commandsMediatorService;

    @Mock
    private Consumer<String, String> consumer;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    void shouldDelegateToCommandsMediatorService() {

        // Arrange
        CommandsMessagesListener listener =
                new CommandsMessagesListener(commandsMediatorService);

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("commands-topic", 0, 0L, "key", "value");

        List<ConsumerRecord<String, String>> records = List.of(record);

        // Act
        listener.onMessage(records, acknowledgment, consumer);

        // Assert
        verify(commandsMediatorService, times(1))
                .evaluate(records, acknowledgment, consumer);

        verifyNoMoreInteractions(commandsMediatorService);
        verifyNoInteractions(acknowledgment);
    }

    @Test
    void shouldHandleEmptyRecords() {

        CommandsMessagesListener listener =
                new CommandsMessagesListener(commandsMediatorService);

        List<ConsumerRecord<String, String>> records = List.of();

        Consumer<String, String> consumer = mock(Consumer.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        listener.onMessage(records, acknowledgment, consumer);

        verify(commandsMediatorService)
                .evaluate(records, acknowledgment, consumer);
    }
}