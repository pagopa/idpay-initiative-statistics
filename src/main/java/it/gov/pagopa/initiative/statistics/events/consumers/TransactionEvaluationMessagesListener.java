package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.trx.TransactionEvaluationStatisticsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionEvaluationMessagesListener extends BaseStatisticsEvaluatorMessagesListener {
    public TransactionEvaluationMessagesListener(TransactionEvaluationStatisticsService transactionEvaluationStatisticsService) {
        super(transactionEvaluationStatisticsService);
    }
}
