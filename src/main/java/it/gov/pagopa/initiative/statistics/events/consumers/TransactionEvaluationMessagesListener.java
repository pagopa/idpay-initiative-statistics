package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.trx.TransactionEvaluationStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Slf4j
public class TransactionEvaluationMessagesListener implements BatchAcknowledgingMessageListener<String, String> {

    private final TransactionEvaluationStatisticsService transactionEvaluationStatisticsService;

    public TransactionEvaluationMessagesListener(TransactionEvaluationStatisticsService transactionEvaluationStatisticsService) {
        this.transactionEvaluationStatisticsService = transactionEvaluationStatisticsService;
    }

    @Override
    public void onMessage(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        //TODO
        System.out.println(records.stream().map(ConsumerRecord::value).toList());

        if(acknowledgment!=null){
            acknowledgment.acknowledge();
        }
    }
}
