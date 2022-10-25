package it.gov.pagopa.initiative.statistics.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.math.BigDecimal;

public interface InitiativeStatRepository extends MongoRepository<InitiativeStatistics, String> {
    UpdateResult updateOnboardingCount(String initiatiativeId, long inc, int partition, long offset);
    UpdateResult updateAccruedRewards(String initiatiativeId, BigDecimal rewardEuro , int partition, long offset);

    long retrieveOnboardingOutcomeCommittedOffset(String initiativeId, int partition);
    long retrieveTransactionEvaluationCommittedOffset(String initiativeId, int partition);
}
