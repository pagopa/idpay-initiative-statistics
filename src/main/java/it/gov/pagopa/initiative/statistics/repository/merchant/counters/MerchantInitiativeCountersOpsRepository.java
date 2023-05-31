package it.gov.pagopa.initiative.statistics.repository.merchant.counters;

import java.math.BigDecimal;

public interface MerchantInitiativeCountersOpsRepository {
    long retrieveMerchantCountersTransactionCommittedOffset(String counterId, int partition);
    void updateCountersFromTransaction(String initiativeId, BigDecimal amount, Long trxs, int partition, long offset);
}
