package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.dto.events.OnboardingOutcomeDTO;
import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.service.onboarding.OnboardingStatisticsService;
import it.gov.pagopa.initiative.statistics.test.fakers.OnboardingOutcomeDTOFaker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

class OnboardingOutcomeMessagesListenerTest extends BaseStatisticsMessagesListenerTest {

    @SpyBean
    private OnboardingStatisticsService onboardingStatisticsServiceSpy;

    @Test
    @Override
    void test(){
        super.test();
    }

    @Override
    protected StatisticsEvaluationService getStatisticsEvaluationServiceSpy() {
        return onboardingStatisticsServiceSpy;
    }

    @Override
    protected String getStatisticsMessagesTopic() {
        return topicOnboardingOutcome;
    }

    @Override
    protected String getStatisticsMessagesGroupId() {
        return groupIdOnboardingOutcome;
    }

    @Override
    protected List<OnboardingOutcomeDTO> buildValidEntities(int bias, int size, String initiativeId) {
        return buildValidOnboardinOutcomesEntities(bias, size, initiativeId);
    }

    @Override
    protected List<OnboardingOutcomeDTO> buildSkippedEntities(int bias, int size) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> {
                    OnboardingOutcomeDTO out = OnboardingOutcomeDTOFaker.mockInstance(i, INITIATIVEID1);
                    out.setStatus("ONBOARDING_KO");
                    return out;
                })
                .toList();
    }

    @Override
    protected List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> getErrorUseCases() {
        return errorUseCases;
    }

    @Override
    protected Function<InitiativeStatistics, Long> getGetterCounter() {
        return InitiativeStatistics::getOnboardedCitizenCount;
    }

    @Override
    protected BiConsumer<InitiativeStatistics, Long> getSetterCounter() {
        return InitiativeStatistics::setOnboardedCitizenCount;
    }

    @Override
    protected Function<InitiativeStatistics, List<CommittedOffset>> getGetterStatisticsCommittedOffsets() {
        return InitiativeStatistics::getOnboardingOutcomeCommittedOffsets;
    }

    @Override
    protected BiConsumer<InitiativeStatistics, List<CommittedOffset>> getSetterStatisticsCommittedOffsets() {
        return InitiativeStatistics::setOnboardingOutcomeCommittedOffsets;
    }

    @Override
    protected long getExpectedCounterValue(int validMsgs) {
        return validMsgs;
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
    //endregion
}
