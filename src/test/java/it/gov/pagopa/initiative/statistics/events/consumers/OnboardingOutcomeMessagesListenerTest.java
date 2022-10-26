package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import it.gov.pagopa.initiative.statistics.dto.events.OnboardingOutcomeDTO;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.ErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.onboarding.OnboardingStatisticsService;
import it.gov.pagopa.initiative.statistics.test.fakers.OnboardingOutcomeDTOFaker;
import it.gov.pagopa.initiative.statistics.test.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

class OnboardingOutcomeMessagesListenerTest extends BaseIntegrationTest {

    private static final String INITIATIVEID1 = "INITIATIVEID1";
    private static final String INITIATIVEID2 = "INITIATIVEID2";

    @SpyBean
    private OnboardingStatisticsService onboardingStatisticsServiceSpy;
    @SpyBean
    private ErrorNotifierService errorNotifierServiceSpy;

    @Autowired
    private InitiativeStatRepository initiativeStatRepository;

    @AfterEach
    void clearData() {
        initiativeStatRepository.deleteAll();
    }

    @Test
    void test() {
        checkJustNotValidMsgsBehavior();
        checkOffsetSkipBehavior();

        int validMsgs = 1000; // use even number
        int skippedMsgs = 100;
        int notValidMsgs = errorUseCases.size();
        long maxWaitingMs = 30000;

        int halfValidMsgs = validMsgs / 2;

        List<String> msgs = new ArrayList<>(buildValidPayloads(0, halfValidMsgs, INITIATIVEID1));
        msgs.addAll(buildValidPayloads(0, halfValidMsgs, INITIATIVEID2));
        msgs.addAll(IntStream.range(0, notValidMsgs).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        msgs.addAll(buildValidPayloads(halfValidMsgs, halfValidMsgs, INITIATIVEID2));
        msgs.addAll(buildValidPayloads(halfValidMsgs, halfValidMsgs, INITIATIVEID1));
        msgs.addAll(buildSkippedPayloads(validMsgs, skippedMsgs));

        long timePublishOnboardingStart = System.currentTimeMillis();
        msgs.forEach(p -> publishIntoEmbeddedKafka(topicOnboardingOutcome, null, null, p));
        long timePublishingOnboardingRequest = System.currentTimeMillis();

        Assertions.assertEquals(validMsgs, waitForCounterResult(INITIATIVEID1, validMsgs, maxWaitingMs));
        long timeCounterUpdated = System.currentTimeMillis();
        Assertions.assertEquals(validMsgs, waitForCounterResult(INITIATIVEID2, validMsgs, maxWaitingMs));

        int expectedTotalSentMessages = msgs.size() + 2; // +2 due to initial published records: offset skip check and notValidMsg

        verifyPartitionOffsetStored(expectedTotalSentMessages, INITIATIVEID1, true);
        verifyPartitionOffsetStored(expectedTotalSentMessages, INITIATIVEID2, false);

        checkErrorsPublished(notValidMsgs, maxWaitingMs, errorUseCases);

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
                timePublishingOnboardingRequest - timePublishOnboardingStart,
                timeCounterUpdated - timePublishingOnboardingRequest,
                timeCounterUpdated - timePublishOnboardingStart
        );

        checkCommittedOffsets(topicOnboardingOutcome, groupIdOnboardingOutcome, expectedTotalSentMessages);
    }

    /** expecting not commit and not notify */
    private void checkJustNotValidMsgsBehavior() {
        publishIntoEmbeddedKafka(topicOnboardingOutcome, 0, null, null, "PROVA");

        waitForEvaluateInvocationTimes("PROVA");

        Assertions.assertEquals(Collections.emptyList(), initiativeStatRepository.findAll());
        Mockito.verifyNoInteractions(errorNotifierServiceSpy);
    }

    private void checkOffsetSkipBehavior() {
        InitiativeStatistics stored = InitiativeStatistics.builder()
                .initiativeId(INITIATIVEID1)
                .onboardingOutcomeCommittedOffsets(List.of(new InitiativeStatistics.CommittedOffset(0, 1))) // 0 offset will be checkJustNotValidMsgsBehavior useCase, while offset 1 will be the current
                .build();
        initiativeStatRepository.save(stored);

        String payload = buildValidPayloads(-1, 1, INITIATIVEID1).get(0);
        publishIntoEmbeddedKafka(topicOnboardingOutcome, 0, null, null, payload);

        waitForEvaluateInvocationTimes(payload);

        stored.setOrganizationId("ORGANIZATIONID-1");
        stored.setOnboardedCitizenCount(0);
        Assertions.assertEquals(List.of(stored), initiativeStatRepository.findAll());
        Mockito.verifyNoInteractions(errorNotifierServiceSpy);
    }

    private void waitForEvaluateInvocationTimes(String payload) {
        Throwable[] lastException = new Throwable[]{null};
        waitFor(() -> {
                    try {
                        Mockito.verify(onboardingStatisticsServiceSpy).evaluate(Mockito.argThat(r -> payload.equals(r.get(0).value())), Mockito.notNull());
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

    private List<OnboardingOutcomeDTO> buildValidEntities(int bias, int size, String initiativeid) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> OnboardingOutcomeDTOFaker.mockInstance(i, initiativeid))
                .toList();
    }

    private List<String> buildSkippedPayloads(int bias, int size) {
        return buildSkippedEntities(bias, size).stream()
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private List<OnboardingOutcomeDTO> buildSkippedEntities(int bias, int size) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> {
                    OnboardingOutcomeDTO out = OnboardingOutcomeDTOFaker.mockInstance(i, INITIATIVEID1);
                    out.setStatus("ONBOARDING_KO");
                    return out;
                })
                .toList();
    }

    private long waitForCounterResult(String initiativeId, int expectedOnboardedCitizen, long maxWaitingMs) {
        int millisAttemptDelay = 500;
        int maxAttempts = (int) maxWaitingMs / millisAttemptDelay;

        long[] countSaved = {0};
        waitFor(() -> (countSaved[0] = initiativeStatRepository.findById(initiativeId).map(InitiativeStatistics::getOnboardedCitizenCount).orElse(-1)) >= expectedOnboardedCitizen
                , () -> "Expected %d onboarded citizen for initiative %s, read %d".formatted(expectedOnboardedCitizen, initiativeId, countSaved[0])
                , maxAttempts, millisAttemptDelay);
        return countSaved[0];
    }

    private void verifyPartitionOffsetStored(long expectOffsetSum, String initiativeid, boolean assertEquals) {
        InitiativeStatistics result = initiativeStatRepository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(result);

        // -2 because offset start from 0 and we are using 2 partition for test
        long expectedOffsetSum0Based = expectOffsetSum - 2;
        long sum = result.getOnboardingOutcomeCommittedOffsets().stream().mapToLong(InitiativeStatistics.CommittedOffset::getOffset).sum();

        if (assertEquals) {
            Assertions.assertEquals(expectedOffsetSum0Based, sum);
        } else {
            Assertions.assertTrue(expectedOffsetSum0Based>=sum, "Expected at least %d obtained %d".formatted(expectedOffsetSum0Based, sum));
        }
    }

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp getErrorUseCaseIdPatternMatch
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"userId\":\"USERID([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();

    {
        String jsonNotExpected = "{\"userId\":\"USERID0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> jsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[INITIATIVE_STATISTICS_EVALUATION][ONBOARDING_OUTCOME] Unexpected json: {\"userId\":\"USERID0\",unexpectedStructure:0}", jsonNotExpected, null)
        ));

        String jsonNotValid = "{\"userId\":\"USERID1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[INITIATIVE_STATISTICS_EVALUATION][ONBOARDING_OUTCOME] Unexpected json: {\"userId\":\"USERID1\",invalidJson", jsonNotValid, null)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(topicOnboardingOutcome, groupIdOnboardingOutcome, errorMessage, errorDescription, expectedPayload, expectedKey);
    }
    //endregion
}
