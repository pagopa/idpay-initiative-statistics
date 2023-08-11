package it.gov.pagopa.initiative.statistics.service.commands;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

public interface CommandsMediatorService {
    void evaluate(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment, Consumer<?, ?> consumer);
}
