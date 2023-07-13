package it.gov.pagopa.initiative.statistics.events.producers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StatistiscsErrorPublisher implements it.gov.pagopa.common.kafka.service.ErrorPublisher {

    private final KafkaTemplate<String, String> publisher;

    public StatistiscsErrorPublisher(@Qualifier("errors") KafkaTemplate<String, String> errorPublisher) {
        this.publisher = errorPublisher;
    }

    public boolean send(Message<?> message) {
        publisher.send(message)
            .thenAccept(r -> log.debug("[ERROR_MESSAGE_HANDLER] message successfully sent to {}",
                publisher.getDefaultTopic()))
            .exceptionally(e -> {
                    log.error(
                        "[ERROR_MESSAGE_HANDLER] something gone wrong while sending message towards topic {}",
                        publisher.getDefaultTopic(), e);
                  return null;
                }
            );
        return true;
    }
  }
