package it.gov.pagopa.initiative.statistics.service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

public interface StatisticsEvaluationService {
    void evaluate(List<ConsumerRecord<String, String>> records, Consumer<?, ?> consumer);
}
