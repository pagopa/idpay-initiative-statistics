package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.BatchAcknowledgingConsumerAwareMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Slf4j
public abstract class BaseStatisticsEvaluatorMessagesListener implements BatchAcknowledgingConsumerAwareMessageListener<String, String> {

    private final StatisticsEvaluationService statisticsEvaluationService;

    protected BaseStatisticsEvaluatorMessagesListener(StatisticsEvaluationService statisticsEvaluationService) {
        this.statisticsEvaluationService = statisticsEvaluationService;
    }


    @Override
    public void onMessage(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment, Consumer<?, ?> consumer) {
        statisticsEvaluationService.evaluate(records, consumer);
        // not acknowledging, because we are manually doing it
    }

}
