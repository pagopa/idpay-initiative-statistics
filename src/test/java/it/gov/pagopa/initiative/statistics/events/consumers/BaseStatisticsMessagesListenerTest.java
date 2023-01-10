package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.service.ErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.test.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

abstract class BaseStatisticsMessagesListenerTest extends BaseIntegrationTest {

    protected static final String INITIATIVEID1 = "INITIATIVEID1";
    protected static final String INITIATIVEID2 = "INITIATIVEID2";

    @SpyBean
    protected ErrorNotifierService errorNotifierServiceSpy;

    @AfterEach
    void clearData() {
        initiativeStatRepository.deleteAll();
    }
    
    protected abstract StatisticsEvaluationService getStatisticsEvaluationServiceSpy();
    protected abstract String getStatisticsMessagesTopic();
    protected abstract String getStatisticsMessagesGroupId();
    protected abstract List<?> buildValidEntities(int bias, int size, String initiativeid);
    protected abstract List<?> buildSkippedEntities(int bias, int size);
    protected abstract List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> getErrorUseCases();
    protected abstract Function<InitiativeStatistics, Long> getGetterCounter();
    protected abstract BiConsumer<InitiativeStatistics, Long> getSetterCounter();
    protected abstract Function<InitiativeStatistics, List<InitiativeStatistics.CommittedOffset>> getGetterStatisticsCommittedOffsets();
    protected abstract BiConsumer<InitiativeStatistics, List<InitiativeStatistics.CommittedOffset>> getSetterStatisticsCommittedOffsets();

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
        msgs.forEach(p -> publishIntoEmbeddedKafka(getStatisticsMessagesTopic(), null, null, p));
        long timePublishingEnd = System.currentTimeMillis();

        long timeCounterUpdated = checkResults(validMsgs, maxWaitingMs);

        int expectedTotalSentMessages = msgs.size() + 2; // +2 due to initial published records: offset skip check and notValidMsg

        int retry=0;

        while(true) {
            long sumOffsets1 = verifyPartitionOffsetStored(expectedTotalSentMessages, INITIATIVEID1, false);
            long sumOffsets2 = verifyPartitionOffsetStored(expectedTotalSentMessages, INITIATIVEID2, false);

            try {
                Assertions.assertTrue(sumOffsets1 == expectedTotalSentMessages || sumOffsets2 == expectedTotalSentMessages,
                        "Unexpected committed offsets stored into db. Expecting: %d; while initiative1 committed: %d and initiative2 committed %d"
                                .formatted(expectedTotalSentMessages, sumOffsets1, sumOffsets2));

                break;
            } catch (AssertionFailedError e){
                if(retry++>3){
                    throw e;
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

        checkCommittedOffsets(getStatisticsMessagesTopic(), getStatisticsMessagesGroupId(), expectedTotalSentMessages);
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
        publishIntoEmbeddedKafka(getStatisticsMessagesTopic(), 0, null, null, "PROVA");

        waitForEvaluateInvocationTimes("PROVA");

        Assertions.assertEquals(Collections.emptyList(), initiativeStatRepository.findAll());
        Mockito.verifyNoInteractions(errorNotifierServiceSpy);
    }
    /** expecting not processed */
    protected void checkOffsetSkipBehavior() {
        InitiativeStatistics.CommittedOffset committedOffset = new InitiativeStatistics.CommittedOffset(0, 1); // 0 offset will be checkJustNotValidMsgsBehavior useCase, while offset 1 will be the current
        InitiativeStatistics stored = InitiativeStatistics.builder()
                .initiativeId(INITIATIVEID1)
                .build();
        getSetterStatisticsCommittedOffsets().accept(stored, List.of(committedOffset));
        initiativeStatRepository.save(stored);

        String payload = buildValidPayloads(-1, 1, INITIATIVEID1).get(0);
        publishIntoEmbeddedKafka(getStatisticsMessagesTopic(), 0, null, null, payload);

        waitForEvaluateInvocationTimes(payload);

        getSetterCounter().accept(stored, 0L);
        stored.setOrganizationId("ORGANIZATIONID_"+stored.getInitiativeId());

        InitiativeStatistics retrieved = initiativeStatRepository.findById(INITIATIVEID1).orElse(null);
        Assertions.assertNotNull(retrieved);
        Assertions.assertNotNull(retrieved.getLastUpdatedDateTime());

        retrieved.setLastUpdatedDateTime(null);
        Assertions.assertEquals(stored, retrieved);
        Mockito.verifyNoInteractions(errorNotifierServiceSpy);
    }


    private void waitForEvaluateInvocationTimes(String payload) {
        Throwable[] lastException = new Throwable[]{null};
        waitFor(() -> {
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

    private List<String> buildValidPayloads(int bias, int size, String initiativeid) {
        return buildValidEntities(bias, size, initiativeid).stream()
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private List<String> buildSkippedPayloads(int bias, int size) {
        return buildSkippedEntities(bias, size).stream()
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    protected long waitForCounterResult(String initiativeId, String organizationId, long expectedCounterValue, long maxWaitingMs) {
        return waitForCounterResult(initiativeId, organizationId, getGetterCounter(), expectedCounterValue, maxWaitingMs);
    }

    protected long verifyPartitionOffsetStored(long expectOffsetSum, String initiativeid, boolean assertEquals) {
        return verifyPartitionOffsetStored(expectOffsetSum, initiativeid, getGetterStatisticsCommittedOffsets(), assertEquals);
    }

    protected void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(getStatisticsMessagesTopic(), getStatisticsMessagesGroupId(), errorMessage, errorDescription, false, expectedPayload, expectedKey);
    }
}
