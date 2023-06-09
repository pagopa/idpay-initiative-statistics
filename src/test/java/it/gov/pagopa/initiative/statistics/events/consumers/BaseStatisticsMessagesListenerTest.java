package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.service.StatisticsErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.util.Pair;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

abstract class BaseStatisticsMessagesListenerTest<T> extends BaseIntegrationTest {

    protected static final String INITIATIVEID1 = "INITIATIVEID1";
    protected static final String INITIATIVEID2 = "INITIATIVEID2";

    @SpyBean
    protected StatisticsErrorNotifierService statisticsErrorNotifierServiceSpy;

    @AfterEach
    void clearData() {
        getStatRepository().deleteAll();
    }

    protected abstract MongoRepository<T, String> getStatRepository();

    protected abstract StatisticsEvaluationService getStatisticsEvaluationServiceSpy();
    protected abstract String getStatisticsMessagesTopic();
    protected abstract String getStatisticsMessagesGroupId();
    protected abstract List<?> buildValidEntities(int bias, int size, String initiativeId);
    protected abstract List<?> buildSkippedEntities(int bias, int size);
    protected abstract List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> getErrorUseCases();
    protected abstract Function<T, Long> getGetterCounter();
    protected abstract BiConsumer<T, Long> getSetterCounter();
    protected abstract Function<T, List<CommittedOffset>> getGetterStatisticsCommittedOffsets();
    protected abstract BiConsumer<T, List<CommittedOffset>> getSetterStatisticsCommittedOffsets();

    protected abstract long getExpectedCounterValue(int validMsgs);

    void test() {
        checkJustNotValidMsgsBehavior();
        checkOffsetSkipBehavior();

        int validMsgs = 1000; // use even number
        int skippedMsgs = 100;
        int notValidMsgs = getErrorUseCases().size();
        long maxWaitingMs = 30000;

        int halfValidMsgs = validMsgs / 2;

        List<String> msgs = new ArrayList<>(buildValidPayloads(0, halfValidMsgs, INITIATIVEID1));
        msgs.addAll(buildValidPayloads(0, halfValidMsgs, INITIATIVEID2));
        msgs.addAll(IntStream.range(0, notValidMsgs).mapToObj(i -> getErrorUseCases().get(i).getFirst().get()).toList());
        msgs.addAll(buildValidPayloads(halfValidMsgs, halfValidMsgs, INITIATIVEID2));
        msgs.addAll(buildValidPayloads(halfValidMsgs, halfValidMsgs, INITIATIVEID1));
        msgs.addAll(buildSkippedPayloads(validMsgs, skippedMsgs));

        long timePublishStart = System.currentTimeMillis();
        msgs.forEach(p -> publishIntoEmbeddedKafka(null, null, p));
        long timePublishingEnd = System.currentTimeMillis();

        long timeCounterUpdated = checkResults(validMsgs, maxWaitingMs);

        int expectedTotalSentMessages = msgs.size() + 2; // +2 due to initial published records: offset skip check and notValidMsg

        int retry=0;

        while(true) {
            long sumOffsets1 = verifyPartitionOffsetStored(expectedTotalSentMessages, INITIATIVEID1, false);
            long sumOffsets2 = verifyPartitionOffsetStored(expectedTotalSentMessages, INITIATIVEID2, false);

            String errorMsg = "Unexpected committed offsets stored into db. Expecting: %d; while initiative1 committed: %d and initiative2 committed %d (after %d retries)"
                    .formatted(expectedTotalSentMessages, sumOffsets1, sumOffsets2, retry);

            try {
                Assertions.assertTrue(sumOffsets1 == expectedTotalSentMessages || sumOffsets2 == expectedTotalSentMessages,
                        errorMsg);

                break;
            } catch (AssertionFailedError e){
                if(retry++>3){
                    throw e;
                } else {
                    System.out.printf("%s %s%n", LocalDateTime.now(), errorMsg);
                    TestUtils.wait(500, TimeUnit.MILLISECONDS);
                }
            }
        }

        checkErrorsPublished(notValidMsgs, maxWaitingMs, getErrorUseCases());

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d +%d) trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                msgs.size(),
                validMsgs * 2,
                notValidMsgs,
                skippedMsgs,
                timePublishingEnd - timePublishStart,
                timeCounterUpdated - timePublishingEnd,
                timeCounterUpdated - timePublishStart
        );

        kafkaTestUtilitiesService.checkCommittedOffsets(getStatisticsMessagesTopic(), getStatisticsMessagesGroupId(), expectedTotalSentMessages);
    }

    protected long checkResults(int validMsgs, long maxWaitingMs) {
        long expectedCounterValue = getExpectedCounterValue(validMsgs);
        Assertions.assertEquals(expectedCounterValue, waitForCounterResult(INITIATIVEID1, "ORGANIZATIONID_"+INITIATIVEID1, expectedCounterValue, maxWaitingMs));
        long timeCounterUpdated = System.currentTimeMillis();
        Assertions.assertEquals(expectedCounterValue, waitForCounterResult(INITIATIVEID2, "ORGANIZATIONID_"+INITIATIVEID2, expectedCounterValue, maxWaitingMs));
        return timeCounterUpdated;
    }

    /** expecting not commit and not notify */
    protected void checkJustNotValidMsgsBehavior() {
        publishIntoEmbeddedKafka(0, null, "PROVA");

        waitForEvaluateInvocationTimes("PROVA");

        Assertions.assertEquals(Collections.emptyList(), getStatRepository().findAll());
        Mockito.verifyNoInteractions(statisticsErrorNotifierServiceSpy);
    }
    /** expecting not processed */
    protected void checkOffsetSkipBehavior() {
        CommittedOffset committedOffset = new CommittedOffset(0, 1); // 0 offset will be checkJustNotValidMsgsBehavior useCase, while offset 1 will be the current
        T stored = buildStatisticInstance(INITIATIVEID1);
        getSetterStatisticsCommittedOffsets().accept(stored, List.of(committedOffset));
        getStatRepository().save(stored);

        String payload = buildValidPayloads(-1, 1, INITIATIVEID1).get(0);
        publishIntoEmbeddedKafka(0, null, payload);

        waitForEvaluateInvocationTimes(payload);

        getSetterCounter().accept(stored, 0L);
        if (stored instanceof InitiativeStatistics storedInitiativeStatistics) {
            storedInitiativeStatistics.setOrganizationId("ORGANIZATIONID_" + storedInitiativeStatistics.getInitiativeId());
        }

        T retrieved = getStatRepository().findById(buildCounterId(INITIATIVEID1)).orElse(null);
        Assertions.assertNotNull(retrieved);
        checkAndEmptyTimestampFields(retrieved);
        checkAndEmptyTimestampFields(stored);
        Assertions.assertEquals(stored, retrieved);
        Mockito.verifyNoInteractions(statisticsErrorNotifierServiceSpy);
    }

    protected void checkAndEmptyTimestampFields(T retrieved) {}

    protected abstract T buildStatisticInstance(String initiativeId);

    protected void publishIntoEmbeddedKafka(Integer partition, String key, String payload) {
        kafkaTestUtilitiesService.publishIntoEmbeddedKafka(getStatisticsMessagesTopic(), partition, null, key, payload);
    }

    private void waitForEvaluateInvocationTimes(String payload) {
        Throwable[] lastException = new Throwable[]{null};
        TestUtils.waitFor(() -> {
                    try {
                        Mockito.verify(getStatisticsEvaluationServiceSpy()).evaluate(Mockito.argThat(r -> payload.equals(r.get(0).value())), Mockito.notNull());
                        return true;
                    } catch (Throwable e) {
                        lastException[0] = e;
                        return false;
                    }
                },
                () -> "Cannot verify not valid msgs behavior: " + lastException[0].toString(),
                10,
                200);
    }

    private List<String> buildValidPayloads(int bias, int size, String initiativeId) {
        return buildValidEntities(bias, size, initiativeId).stream()
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private List<String> buildSkippedPayloads(int bias, int size) {
        return buildSkippedEntities(bias, size).stream()
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    protected long waitForCounterResult(String initiativeId, String organizationId, long expectedCounterValue, long maxWaitingMs) {
        return waitForCounterResult(initiativeId, organizationId, getGetterCounter(), expectedCounterValue, maxWaitingMs, getStatRepository());
    }

    protected long verifyPartitionOffsetStored(long expectOffsetSum, String initiativeid, boolean assertEquals) {
        return verifyPartitionOffsetStored(expectOffsetSum, initiativeid, getGetterStatisticsCommittedOffsets(), assertEquals, getStatRepository());
    }

    protected void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(getStatisticsMessagesTopic(), getStatisticsMessagesGroupId(), errorMessage, errorDescription, expectedPayload, expectedKey, false, true);
    }
}
