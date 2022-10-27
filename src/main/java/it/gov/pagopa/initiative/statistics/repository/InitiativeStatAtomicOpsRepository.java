package it.gov.pagopa.initiative.statistics.repository;

import java.math.BigDecimal;

public interface InitiativeStatAtomicOpsRepository {

    long retrieveOnboardingOutcomeCommittedOffset(String initiativeId, String organizationId, int partition);
    long retrieveTransactionEvaluationCommittedOffset(String initiativeId, int partition);

    void updateOnboardingCount(String initiatiativeId, long inc, int partition, long offset);
    void updateAccruedRewards(String initiatiativeId, BigDecimal rewardEuro , int partition, long offset);
}
