package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.merchant.counters.trx.MerchantTransactionService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MerchantCountersTransactionMessagesListener extends BaseStatisticsEvaluatorMessagesListener {
    public MerchantCountersTransactionMessagesListener(MerchantTransactionService merchantTransactionService) {
        super(merchantTransactionService);
    }
}
