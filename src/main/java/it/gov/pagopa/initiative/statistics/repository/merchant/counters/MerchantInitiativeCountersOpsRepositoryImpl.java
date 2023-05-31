package it.gov.pagopa.initiative.statistics.repository.merchant.counters;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import lombok.extern.slf4j.Slf4j;
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
public class MerchantInitiativeCountersOpsRepositoryImpl implements MerchantInitiativeCountersOpsRepository{

    private final MongoTemplate client;

    public MerchantInitiativeCountersOpsRepositoryImpl(MongoTemplate client) {
        this.client = client;
    }

    @Override
    public long retrieveMerchantCountersTransactionCommittedOffset(String counterId, int partition) {
        return retrieveOffset(counterId, partition, MerchantInitiativeCounters::getTrxCommittedOffsets, MerchantInitiativeCounters.Fields.trxCommittedOffsets);
    }

    private Long retrieveOffset(String counterId, int partition, Function<MerchantInitiativeCounters, List<CommittedOffset>> commitsgetter, String commitsField){
        MerchantInitiativeCounters entity = createRecordIfNotExists(counterId);
        Long out = null;

        List<CommittedOffset> commits = commitsgetter.apply(entity);
        if(commits != null){
            out = commits.stream().filter(c->partition == c.getPartition()).map(CommittedOffset::getOffset).findFirst().orElse(null);
        }

        if(out == null){
            out=-1L;

            client.updateFirst(
                    Query.query(Criteria.where(MerchantInitiativeCounters.Fields.id).is(counterId)),
                    new Update()
                            .push(commitsField, new CommittedOffset(partition, out)),
                    InitiativeStatistics.class
            );
        }
        return out;
    }

    private MerchantInitiativeCounters createRecordIfNotExists(String counterId) {
        String merchantId = MerchantInitiativeCounters.splitId(counterId)[0];
        String initiativeId = MerchantInitiativeCounters.splitId(counterId)[1];

        MerchantInitiativeCounters result = client.findById(counterId, MerchantInitiativeCounters.class);

        if(result==null){
            result = new MerchantInitiativeCounters();
            result.setId(counterId);
            result.setMerchantId(merchantId);
            result.setInitiativeId(initiativeId);

            Update updateQuery = new Update()
                    .set(MerchantInitiativeCounters.Fields.id, counterId)
                    .set(MerchantInitiativeCounters.Fields.merchantId, merchantId)
                    .set(MerchantInitiativeCounters.Fields.initiativeId, initiativeId);

            try {
                client.upsert(
                        Query.query(Criteria.where(MerchantInitiativeCounters.Fields.id).is(counterId)),
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
    public void updateCountersFromTransaction(String initiativeId, BigDecimal amount, Long trxs, int partition, long offset) {
        Map<String, Number> incrementsMap = Map.of(
                MerchantInitiativeCounters.Fields.totalAmount, amount,
                MerchantInitiativeCounters.Fields.trxNumber, trxs
        );
        incrementCounterAndPartitionCommittedOffsets(initiativeId, incrementsMap, MerchantInitiativeCounters.Fields.trxCommittedOffsets, partition, offset);
    }

    private void incrementCounterAndPartitionCommittedOffsets(String counterId, Map<String, Number> fieldCounter2Inc, String fieldPartitionCommitted, int partition, long offset) {
        Update update = new Update()
                .set("%s.$.%s".formatted(fieldPartitionCommitted, CommittedOffset.Fields.offset), offset);
        fieldCounter2Inc.forEach(update::inc);

        UpdateResult updateResult = client.updateFirst(
                Query.query(
                        Criteria.where(MerchantInitiativeCounters.Fields.id).is(counterId)
                                .and("%s.%s".formatted(fieldPartitionCommitted, CommittedOffset.Fields.partition)).is(partition)
                ),
                update,
                MerchantInitiativeCounters.class
        );
        if(updateResult.getModifiedCount()>0){
            log.info("[INITIATIVE_STATISTICS_EVALUATION]{} Counter updated for merchantId-initiativeId {} inc by {} and committed offset {}-{}", fieldCounter2Inc.keySet().stream().map("[INC_%s]"::formatted).collect(Collectors.joining()), counterId, fieldCounter2Inc, partition, offset);
        } else {
            throw new IllegalStateException("[INITIATIVE_STATISTICS_EVALUATION]%s Counter increase called on not existent counterId-topicPartition: %s %s".formatted(fieldCounter2Inc.keySet().stream().map("[INC_%s]"::formatted).collect(Collectors.joining()), counterId, partition));
        }
    }
}
