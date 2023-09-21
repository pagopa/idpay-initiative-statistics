package it.gov.pagopa.initiative.statistics.repository.merchant.counters;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters.Fields;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public long retrieveMerchantCountersTransactionCommittedOffset(String counterId, String merchantId, String initiativeId, int partition) {
        return retrieveOffset(counterId, merchantId, initiativeId, partition, MerchantInitiativeCounters::getTrxCommittedOffsets, MerchantInitiativeCounters.Fields.trxCommittedOffsets);
    }

    @Override
    public long retrieveMerchantCountersNotificationCommittedOffset(String counterId, String merchantId, String initiativeId, int partition) {
        return retrieveOffset(counterId, merchantId, initiativeId, partition, MerchantInitiativeCounters::getRewardNotificationCommittedOffsets, MerchantInitiativeCounters.Fields.rewardNotificationCommittedOffsets);
    }

    @Override
    public List<MerchantInitiativeCounters> deletePaged(String initiativeId, int pageSize) {
        log.trace("[DELETE_PAGED] Deleting merchant initiative counters in pages");
        Pageable pageable = PageRequest.of(0, pageSize);
        return client.findAllAndRemove(
                Query.query(Criteria.where(Fields.initiativeId).is(initiativeId)).with(pageable),
                MerchantInitiativeCounters.class
        );
    }

    private Long retrieveOffset(String counterId, String merchantId, String initiativeId, int partition, Function<MerchantInitiativeCounters, List<CommittedOffset>> commitsgetter, String commitsField){
        MerchantInitiativeCounters entity = createRecordIfNotExists(counterId, merchantId, initiativeId);
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
                    MerchantInitiativeCounters.class
            );
        }
        return out;
    }

    private MerchantInitiativeCounters createRecordIfNotExists(String counterId, String merchantId, String initiativeId) {

        MerchantInitiativeCounters result = client.findById(counterId, MerchantInitiativeCounters.class);

        if(result==null){
            result = new MerchantInitiativeCounters(merchantId, initiativeId);

            Update updateQuery = new Update()
                    .set(MerchantInitiativeCounters.Fields.id, counterId)
                    .set(MerchantInitiativeCounters.Fields.merchantId, merchantId)
                    .set(MerchantInitiativeCounters.Fields.initiativeId, initiativeId)
                    .currentDate(MerchantInitiativeCounters.Fields.lastUpdatedDateTime);

            try {
                client.upsert(
                        Query.query(Criteria.where(MerchantInitiativeCounters.Fields.id).is(counterId)),
                        updateQuery,
                        MerchantInitiativeCounters.class
                );
            } catch (DuplicateKeyException e){
                // Do nothing!
            }
        }

        return result;
    }

    @Override
    public void updateCountersFromTransaction(String counterId, BigDecimal amount, Long trxs, int partition, long offset) {
        Map<String, Long> incrementsMap = Map.of(
                MerchantInitiativeCounters.Fields.totalProvidedCents, CommonUtilities.euroToCents(amount),
                MerchantInitiativeCounters.Fields.trxNumber, trxs
        );
        incrementCounterAndPartitionCommittedOffsets(counterId, incrementsMap, MerchantInitiativeCounters.Fields.trxCommittedOffsets, partition, offset);
    }

    @Override
    public void updateCountersFromRewardNotification(String counterId, Long refunded, Long trxs, int partition, long offset) {
        Map<String, Long> incrementsMap = Map.of(
                MerchantInitiativeCounters.Fields.totalRefundedCents, refunded,
                MerchantInitiativeCounters.Fields.refundedNumber, trxs
        );
        incrementCounterAndPartitionCommittedOffsets(counterId, incrementsMap, MerchantInitiativeCounters.Fields.rewardNotificationCommittedOffsets, partition, offset);
    }

    private void incrementCounterAndPartitionCommittedOffsets(String counterId, Map<String, Long> fieldCounter2Inc, String fieldPartitionCommitted, int partition, long offset) {
        Update update = new Update()
                .set("%s.$.%s".formatted(fieldPartitionCommitted, CommittedOffset.Fields.offset), offset)
                .currentDate(MerchantInitiativeCounters.Fields.lastUpdatedDateTime);
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
