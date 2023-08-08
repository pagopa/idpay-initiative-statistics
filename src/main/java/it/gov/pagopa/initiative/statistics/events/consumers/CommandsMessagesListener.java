package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.commands.CommandsMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.BatchAcknowledgingConsumerAwareMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Slf4j
public class CommandsMessagesListener implements BatchAcknowledgingConsumerAwareMessageListener<String, String> {
    private final CommandsMediatorService commandsMediatorService;

    public CommandsMessagesListener(CommandsMediatorService commandsMediatorService) {
        this.commandsMediatorService = commandsMediatorService;
    }

    public void onMessage(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment, Consumer<?, ?> consumer) {
        commandsMediatorService.evaluate(records, acknowledgment, consumer);
    }
}
