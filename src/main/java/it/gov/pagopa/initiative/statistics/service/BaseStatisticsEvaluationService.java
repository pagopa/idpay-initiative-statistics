package it.gov.pagopa.initiative.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A service that will accept a list of messages, from which will extract initiative statistics
 * @param <E> the type of the payload
 * @param <I> the type of the entity related 1-to-1 with the initiative, extracted from {@link E}
 */
@Slf4j
public abstract class BaseStatisticsEvaluationService<E, I> implements StatisticsEvaluationService {

    private final String applicationName;
    private final ObjectReader objectReader;

    protected BaseStatisticsEvaluationService(String applicationName, ObjectMapper objectMapper) {
        this.applicationName = applicationName;
        this.objectReader = objectMapper.readerFor(getRecordClass());
    }

    protected abstract Class<E> getRecordClass();

    @Override
    public void evaluate(List<ConsumerRecord<String, String>> records, Consumer<?, ?> consumer){
        log.debug("[INITIATIVE_STATISTICS_EVALUATION][{}] Evaluating {} records", getFlowName(), records.size());

        records.parallelStream()
                // grouping by partition
                .collect(Collectors.groupingBy(ConsumerRecord::partition))
                // evaluating partition records
                .forEach((p, rs) -> evaluatePartitionRecords(p, rs, consumer));
    }

    /** It will check if the current record is not a RETRY of another application */
    private boolean isNotRetry(Pair<ConsumerRecord<String, String>, E> r2e) {
        ConsumerRecord<String, String> r = r2e.getKey();

        Header appNameRecord = r.headers().lastHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME);
        Header retry = r.headers().lastHeader("RETRY");
        boolean out = retry == null || (appNameRecord != null && applicationName.equals(new String(appNameRecord.value(), StandardCharsets.UTF_8)));
        if(!out){
            log.info("[INITIATIVE_STATISTICS_EVALUATION][{}] Skipping record because other application retry: appName: {}, retry: {}"
                    , getFlowName(),
                    appNameRecord!=null? new String(appNameRecord.value(), StandardCharsets.UTF_8) : "",
                    new String(retry.value(), StandardCharsets.UTF_8)
                    );
        }
        return out;
    }

    /** It will evaluate partition records, committing its offsets at the end */
    @SuppressWarnings("java:S3864") // suppressing peek warning: in this case the optimization described will not be performed
    private void evaluatePartitionRecords(int partition, List<ConsumerRecord<String, String>> records, Consumer<?, ?> consumer){
        log.debug("[INITIATIVE_STATISTICS_EVALUATION][{}] Evaluating partition {}: {} records", getFlowName(), partition, records.size());

        List<Triple<ConsumerRecord<String, String>, String, Throwable>> errorRecords = new ArrayList<>();

        AtomicLong maxOffsetAtomic = new AtomicLong(-1);

        Map<String, List<Triple<ConsumerRecord<String, String>, String, I>>> groupByInitiative = records.parallelStream()
                // deserializing and returning pair of record and entity
                .map(r -> {
                    try {
                        return Pair.of(r, deserialize(r.value()));
                    } catch (IOException e) {
                        errorRecords.add(Triple.of(r,
                                "[INITIATIVE_STATISTICS_EVALUATION][%s] Unexpected json: %s".formatted(getFlowName(), r.value()),
                                e));
                        return null;
                    }
                })
                // skipping deserialization failed records
                .filter(Objects::nonNull)
                // storing maxOffsetAtomic of valid records
                .peek(r2e -> maxOffsetAtomic.getAndUpdate(o -> Math.max(o, r2e.getKey().offset())))
                // skipping retry messages scheduled by other application
                .filter(this::isNotRetry)
                // transforming the record2entity stream into a pair record2initiativeBased stream
                .flatMap(r2e -> toInitiativeBasedEntityStream(r2e.getValue()).map(i -> Triple.of(r2e.getKey(), getInitiativeId(i), i)))
                // skipping entities without initiativeId
                .filter(r2id2i -> {
                    boolean hasInitiativeId = !StringUtils.isEmpty(r2id2i.getMiddle());
                    if (!hasInitiativeId) {
                        log.warn("[INITIATIVE_STATISTICS_EVALUATION][{}] Cannot find initiativeId in input entity: {} - {}", getFlowName(), r2id2i.getRight(), r2id2i.getLeft().value());
                    }
                    return hasInitiativeId;
                })
                // grouping by  initiativeId
                .collect(Collectors.groupingBy(Triple::getMiddle));

        long maxOffset = maxOffsetAtomic.get();
        groupByInitiative
                .entrySet().stream()
                // evaluating last committed offset for initiativeId
                .map(p -> {
                    String initiativeId = p.getKey();

                    log.trace("[INITIATIVE_STATISTICS_EVALUATION][{}] Evaluating initiativeId {} read from partition {}: {} records", getFlowName(), initiativeId, partition, records.size());

                    long lastCommittedOffset = retrieveLastProcessedOffset(initiativeId, partition, p.getValue().get(0).getRight());

                    log.trace("[INITIATIVE_STATISTICS_EVALUATION][{}] Evaluating initiativeId {} read from partition {}: {} records; last processed offset {}", getFlowName(), initiativeId, partition, records.size(), lastCommittedOffset);

                    Pair<String, List<I>> out = Pair.of(
                            initiativeId,
                            p.getValue().stream().filter(r2i -> r2i.getLeft().offset() > lastCommittedOffset).map(Triple::getRight).toList()
                    );

                    log.debug("[INITIATIVE_STATISTICS_EVALUATION][{}] Evaluating initiativeId {} of {} read from partition {}: {} records; last processed offset {}", getFlowName(), initiativeId, partition, out.getValue().size(), records.size(), lastCommittedOffset);

                    return out;
                })
                // evaluating each initiative
                .forEach(i2e -> evaluateInitiative(i2e.getKey(), i2e.getValue(), partition, maxOffset));

        if(!records.isEmpty() && consumer!=null && maxOffset > -1){
            log.info("[INITIATIVE_STATISTICS_EVALUATION][{}] Committing partition {} and offset {}", getFlowName(), partition, maxOffset);
            TopicPartition topicPartition = new TopicPartition(records.get(0).topic(), partition);
            consumer.commitAsync(
                    Map.of(topicPartition, new OffsetAndMetadata(maxOffset+1)) // +1 because we have to indicate the next offset to read
                    , errorRecords.isEmpty() ? null :
                            onCommitNotifyErrors(errorRecords.stream()
                                    .filter(r-> {
                                        boolean toNotify = r.getLeft().offset() <= maxOffset;
                                        if(!toNotify){
                                            log.warn("[INITIATIVE_STATISTICS_EVALUATION][{}] skipping error publishing because there are not next valid messages: thus it will be read again; payload:{}, errorDescription:{}", getFlowName(), r.getLeft().value(), r.getMiddle(), r.getRight());
                                        }
                                        return toNotify;
                                    })
                                    .toList())
            );
        }
    }

    /** The name of the business logic flow to print when logging */
    protected abstract String getFlowName();

    /** It will retrieve the last processed offset */
    protected abstract long retrieveLastProcessedOffset(String initiativeId, int partition, I right);

    /** In case of errors reading a message */
    protected abstract void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception);

    private E deserialize(String payload) throws JsonProcessingException {
        return objectReader.readValue(payload);
    }

    /** Given the input entity ({@link E}), it will build a {@link Stream} of entities associated 1-to-1 to the initiatives ({@link I}) */
    protected abstract Stream<I> toInitiativeBasedEntityStream(E e);

    /** to extract the initiativeId from {@link I} */
    protected abstract String getInitiativeId(I t);

    /** It will evaluate and update initiative statistics */
    protected abstract void evaluateInitiative(String initiativeId, List<I> records, int partition, long maxOffset);

    private OffsetCommitCallback onCommitNotifyErrors(List<Triple<ConsumerRecord<String, String>, String, Throwable>> errorRecords) {
        return (offsets, exception) -> {
            if(exception==null){
                errorRecords.forEach(e -> onRecordError2notify(e.getLeft(), e.getMiddle(), e.getRight()));
            } else {
                log.error("[INITIATIVE_STATISTICS_EVALUATION][{}] Offset commit with offsets {} failed", getFlowName(), offsets, exception);
            }
        };
    }
}
