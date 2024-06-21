package it.gov.pagopa.initiative.statistics.repository;

public interface InitiativeStatAtomicOpsRepository {

    long retrieveOnboardingOutcomeCommittedOffset(String initiativeId, String organizationId, int partition);
    long retrieveTransactionEvaluationCommittedOffset(String initiativeId, String organizationId, int partition);

    void updateOnboardingCount(String initiativeId, long inc, int partition, long offset);
    void updateAccruedRewards(String initiativeId, Long rewardCents, Long trxs, int partition, long offset);
}
