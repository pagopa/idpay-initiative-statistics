package it.gov.pagopa.initiative.statistics.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("unused") // used by Spring Data because it has the same name of the interface + "Impl", and this interface is extended by a @Repository
public class InitiativeStatAtomicOpsRepositoryImpl implements InitiativeStatAtomicOpsRepository {

    public static final String FIELD_INITIATIVE_ID = InitiativeStatistics.Fields.initiativeId;
    public static final String FIELD_LAST_UPDATE_DATE = InitiativeStatistics.Fields.lastUpdatedDateTime;

    public static final String FIELD_ONBOARDED_CITIZEN_COUNT = InitiativeStatistics.Fields.onboardedCitizenCount;
    public static final String FIELD_ONBOARDING_OUTCOME_COMMITTED_OFFSETS = InitiativeStatistics.Fields.onboardingOutcomeCommittedOffsets;

    public static final String FIELD_ACCRUED_REWARD_CENTS = InitiativeStatistics.Fields.accruedRewardsCents;
    public static final String FIELD_REWARDED_TRXS = InitiativeStatistics.Fields.rewardedTrxs;
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
    public long retrieveTransactionEvaluationCommittedOffset(String initiativeId, String organizationId, int partition) {
        return retrieveOffset(initiativeId, organizationId, partition, InitiativeStatistics::getTransactionEvaluationCommittedOffsets, InitiativeStatistics.Fields.transactionEvaluationCommittedOffsets);
    }

    private Long retrieveOffset(String initiativeId, String organizationId, int partition, Function<InitiativeStatistics, List<CommittedOffset>> commitsgetter, String commitsField){
        InitiativeStatistics entity = createRecordIfNotExists(initiativeId, organizationId);
        Long out = null;

        List<CommittedOffset> commits = commitsgetter.apply(entity);
        if(commits != null){
            out = commits.stream().filter(c->partition == c.getPartition()).map(CommittedOffset::getOffset).findFirst().orElse(null);
        }

        if(out == null){
            out=-1L;

            client.updateFirst(
                    Query.query(Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId)),
                    new Update()
                            .push(commitsField, new CommittedOffset(partition, out)),
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
                    .currentDate(FIELD_LAST_UPDATE_DATE);

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
    public void updateOnboardingCount(String initiativeId, long inc, int partition, long offset) {
        incrementCounterAndPartitionCommittedOffsets(initiativeId, Map.of(FIELD_ONBOARDED_CITIZEN_COUNT, inc), FIELD_ONBOARDING_OUTCOME_COMMITTED_OFFSETS, partition, offset);
    }

    @Override
    public void updateAccruedRewards(String initiativeId, BigDecimal rewardEuro, Long trxs, int partition, long offset) {
        Map<String, Long> incrementsMap = Map.of(
                FIELD_ACCRUED_REWARD_CENTS, CommonUtilities.euroToCents(rewardEuro),
                FIELD_REWARDED_TRXS, trxs
        );
        incrementCounterAndPartitionCommittedOffsets(initiativeId, incrementsMap, FIELD_TRANSACTION_EVALUATION_COMMITTED_OFFSETS, partition, offset);
    }

    private void incrementCounterAndPartitionCommittedOffsets(String initiativeId, Map<String, Long> fieldCounter2Inc, String fieldPartitionCommitted, int partition, long offset) {
        Update update = new Update()
                .set("%s.$.%s".formatted(fieldPartitionCommitted, CommittedOffset.Fields.offset), offset)
                .currentDate(FIELD_LAST_UPDATE_DATE);
        fieldCounter2Inc.forEach(update::inc);

        UpdateResult updateResult = client.updateFirst(
                Query.query(
                        Criteria.where(FIELD_INITIATIVE_ID).is(initiativeId)
                                .and("%s.%s".formatted(fieldPartitionCommitted, CommittedOffset.Fields.partition)).is(partition)
                ),
                update,
                InitiativeStatistics.class
        );
        if(updateResult.getModifiedCount()>0){
            log.info("[INITIATIVE_STATISTICS_EVALUATION]{} Counter updated for initiative {} inc by {} and committed offset {}-{}", fieldCounter2Inc.keySet().stream().map("[INC_%s]"::formatted).collect(Collectors.joining()), initiativeId, fieldCounter2Inc, partition, offset);
        } else {
            throw new IllegalStateException("[INITIATIVE_STATISTICS_EVALUATION]%s Counter increase called on not existent initiativeId-topicPartition: %s %s".formatted(fieldCounter2Inc.keySet().stream().map("[INC_%s]"::formatted).collect(Collectors.joining()), initiativeId, partition));
        }
    }
}
