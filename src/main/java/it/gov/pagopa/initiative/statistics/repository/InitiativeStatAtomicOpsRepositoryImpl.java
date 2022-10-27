package it.gov.pagopa.initiative.statistics.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Slf4j
@SuppressWarnings("unused") // used by Spring Data because it has the same name of the interface + "Impl", and this interface is extended by a @Repository
public class InitiativeStatAtomicOpsRepositoryImpl implements InitiativeStatAtomicOpsRepository {

    public static final String FIELD_INITIATIVE_ID = InitiativeStatistics.Fields.initiativeId;
    public static final String FIELD_LAST_UPDATE_DATE = InitiativeStatistics.Fields.lastUpdatedDateTime;

    public static final String FIELD_ONBOARDED_CITIZEN_COUNT = InitiativeStatistics.Fields.onboardedCitizenCount;
    public static final String FIELD_ONBOARDING_OUTCOME_COMMITTED_OFFSETS = InitiativeStatistics.Fields.onboardingOutcomeCommittedOffsets;

    public static final String FIELD_ACCRUED_REWARD_CENTS = InitiativeStatistics.Fields.accruedRewardsCents;
    public static final String FIELD_TRANSACTION_EVALUATION_COMMITTED_OFFSETS = InitiativeStatistics.Fields.transactionEvaluationCommittedOffsets;

    private final MongoTemplate client;

    public InitiativeStatAtomicOpsRepositoryImpl(MongoTemplate client) {
        this.client = client;
    }

    @Override
    public long retrieveOnboardingOutcomeCommittedOffset(String initiativeId, String organizationId, int partition) {
        return retrieveOffset(initiativeId, organizationId, partition, InitiativeStatistics::getOnboardingOutcomeCommittedOffsets, FIELD_ONBOARDING_OUTCOME_COMMITTED_OFFSETS);
    }

    @Override
    public long retrieveTransactionEvaluationCommittedOffset(String initiativeId, int partition) {
        return retrieveOffset(initiativeId, null, partition, InitiativeStatistics::getTransactionEvaluationCommittedOffsets, InitiativeStatistics.Fields.transactionEvaluationCommittedOffsets);
    }

    private Long retrieveOffset(String initiativeId, String organizationId, int partition, Function<InitiativeStatistics, List<InitiativeStatistics.CommittedOffset>> commitsgetter, String commitsField){
        InitiativeStatistics entity = createRecordIfNotExists(initiativeId, organizationId);
        Long out = null;

        List<InitiativeStatistics.CommittedOffset> commits = commitsgetter.apply(entity);
        if(commits != null){
            out = commits.stream().filter(c->partition == c.getPartition()).map(InitiativeStatistics.CommittedOffset::getOffset).findFirst().orElse(null);
        }

        if(out == null){
            out=-1L;

            client.updateFirst(
                    Query.query(Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId)),
                    new Update()
                            .push(commitsField, new InitiativeStatistics.CommittedOffset(partition, out)),
                    InitiativeStatistics.class
            );
        }
        return out;
    }

    private InitiativeStatistics createRecordIfNotExists(String initiativeId, String organizationId) {
        InitiativeStatistics result = client.findById(initiativeId, InitiativeStatistics.class);

        if(result==null || (StringUtils.isEmpty(result.getOrganizationId()) && !StringUtils.isEmpty(organizationId))){
            if(result==null){
                result = new InitiativeStatistics();
                result.setInitiativeId(initiativeId);
            }
            result.setOrganizationId(organizationId);

            Update updateQuery = new Update()
                    .set(FIELD_INITIATIVE_ID, initiativeId)
                    .set(FIELD_LAST_UPDATE_DATE, LocalDateTime.now());

            if(!StringUtils.isEmpty(organizationId)){
                updateQuery.set(InitiativeStatistics.Fields.organizationId, organizationId);
            }

            try {
                client.upsert(
                        Query.query(Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId)),
                        updateQuery,
                        InitiativeStatistics.class
                );
            } catch (DuplicateKeyException e){
                // Do nothing!
            }
        }

        return result;
    }

    @Override
    public void updateOnboardingCount(String initiatiativeId, long inc, int partition, long offset) {
        incrementCounterAndPartitionCommittedOffsets(initiatiativeId, inc, partition, offset, FIELD_ONBOARDED_CITIZEN_COUNT, FIELD_ONBOARDING_OUTCOME_COMMITTED_OFFSETS);
    }

    @Override
    public void updateAccruedRewards(String initiatiativeId, BigDecimal rewardEuro, int partition, long offset) {
        Long inc = Utils.euro2Cents(rewardEuro);
        incrementCounterAndPartitionCommittedOffsets(initiatiativeId, inc, partition, offset, FIELD_ACCRUED_REWARD_CENTS, FIELD_TRANSACTION_EVALUATION_COMMITTED_OFFSETS);
    }

    private void incrementCounterAndPartitionCommittedOffsets(String initiatiativeId, long inc, int partition, long offset, String fieldCounter, String fieldPartitionCommitted) {
        UpdateResult updateResult = client.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiatiativeId)
                                .and("%s.%s".formatted(fieldPartitionCommitted, InitiativeStatistics.CommittedOffset.Fields.partition)).is(partition)
                ),
                new Update()
                        .inc(fieldCounter, inc)
                        .set("%s.$.%s".formatted(fieldPartitionCommitted, InitiativeStatistics.CommittedOffset.Fields.offset), offset)
                        .set(FIELD_LAST_UPDATE_DATE, LocalDateTime.now()),
                InitiativeStatistics.class
        );
        if(updateResult.getModifiedCount()>0){
            log.info("[INITIATIVE_STATISTICS_EVALUATION][INC_{}] Counter updated for initiative {} inc by {} and committed offset {}-{}", fieldCounter, initiatiativeId, inc, partition, offset);
        } else {
            throw new IllegalStateException("[INITIATIVE_STATISTICS_EVALUATION][INC_%s] Counter increase called on not existent initiativeId-topicPartition: %s %s".formatted(fieldCounter, initiatiativeId, partition));
        }
    }
}
