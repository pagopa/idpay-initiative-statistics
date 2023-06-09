package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.merchant.counters.trx.MerchantTransactionStatisticsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MerchantCountersTransactionMessagesListener extends BaseStatisticsEvaluatorMessagesListener {
    public MerchantCountersTransactionMessagesListener(MerchantTransactionStatisticsService merchantTransactionStatisticsService) {
        super(merchantTransactionStatisticsService);
    }
}
