package it.gov.pagopa.initiative.statistics.repository;

import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public class InitiativeStatAtomicOpsRepositoryImpl implements InitiativeStatAtomicOpsRepository {

    private final MongoTemplate client;

    public InitiativeStatAtomicOpsRepositoryImpl(MongoTemplate client) {
        this.client = client;
    }

    @Override
    public long retrieveOnboardingOutcomeCommittedOffset(String initiativeId, String organizationId, int partition) {
        return retrieveOffset(initiativeId, organizationId, partition, InitiativeStatistics::getOnboardingOutcomeCommittedOffsets, InitiativeStatistics.Fields.onboardingOutcomeCommittedOffsets);
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
                    Query.query(Criteria.where(InitiativeStatistics.Fields.initiativeId).is(initiativeId)),
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
                    .set(InitiativeStatistics.Fields.initiativeId, initiativeId);

            if(!StringUtils.isEmpty(organizationId)){
                updateQuery.set(InitiativeStatistics.Fields.organizationId, organizationId);
            }

            try {
                client.upsert(
                        Query.query(Criteria.where(InitiativeStatistics.Fields.initiativeId).is(initiativeId)),
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
        //TODO
    }

    @Override
    public void updateAccruedRewards(String initiatiativeId, BigDecimal rewardEuro, int partition, long offset) {
        //TODO
    }
}
