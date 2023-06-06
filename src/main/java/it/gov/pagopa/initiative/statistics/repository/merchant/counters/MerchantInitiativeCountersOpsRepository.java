package it.gov.pagopa.initiative.statistics.repository.merchant.counters;

import java.math.BigDecimal;

public interface MerchantInitiativeCountersOpsRepository {
    long retrieveMerchantCountersTransactionCommittedOffset(String counterId, int partition);
    long retrieveMerchantCountersNotificationCommittedOffset(String counterId, int partition);
    void updateCountersFromTransaction(String counterId, BigDecimal amount, Long trxs, int partition, long offset);
    void updateCountersFromRewardNotification(String counterId, Long refunded, Long trxs, int partition, long offset);
}
