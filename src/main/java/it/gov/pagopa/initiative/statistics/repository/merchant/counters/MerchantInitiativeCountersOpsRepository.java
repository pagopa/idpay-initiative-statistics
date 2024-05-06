package it.gov.pagopa.initiative.statistics.repository.merchant.counters;

import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;

import java.util.List;

public interface MerchantInitiativeCountersOpsRepository {
    long retrieveMerchantCountersTransactionCommittedOffset(String counterId, String merchantId, String initiativeId, int partition);
    long retrieveMerchantCountersNotificationCommittedOffset(String counterId, String merchantId, String initiativeId, int partition);
    void updateCountersFromTransaction(String counterId, Long amountCents, Long trxs, int partition, long offset);
    void updateCountersFromRewardNotification(String counterId, Long refunded, Long trxs, int partition, long offset);
    List<MerchantInitiativeCounters> deletePaged(String initiativeId, int pageSize);
}
