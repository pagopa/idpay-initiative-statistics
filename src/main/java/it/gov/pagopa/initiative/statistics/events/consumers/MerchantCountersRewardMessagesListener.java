package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.merchant.counters.notification.MerchantNotificationStatisticsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MerchantCountersRewardMessagesListener extends BaseStatisticsEvaluatorMessagesListener {
    public MerchantCountersRewardMessagesListener(MerchantNotificationStatisticsService merchantNotificationStatisticsService) {
        super(merchantNotificationStatisticsService);
    }
}
