package it.gov.pagopa.initiative.statistics.service.onboarding;

import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationServiceTest;
import it.gov.pagopa.initiative.statistics.service.ErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.test.fakers.OnboardingOutcomeDTOFaker;
import it.gov.pagopa.initiative.statistics.test.utils.TestUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OnboardingStatisticsServiceTest extends BaseStatisticsEvaluationServiceTest {

    @Mock private ErrorNotifierService errorNotifierServiceMock;
    @Mock private InitiativeStatRepository initiativeStatRepositoryMock;
    @Mock private Consumer<?,?> consumerMock;

    @Test
    void test(){
        invokeService("APPNAME", consumerMock, 5, 7, errorNotifierServiceMock);

        Mockito.verify(errorNotifierServiceMock, Mockito.times(expectedErrorNotification)).notifyOnboardingOutcome(Mockito.any()
                , Mockito.argThat(description -> description.startsWith("[INITIATIVE_STATISTICS_EVALUATION][ONBOARDING_OUTCOME] Unexpected json: "))
                , Mockito.eq(false), Mockito.any());
    }

    @Override
    protected StatisticsEvaluationService getStatisticsEvaluationServiceImpl() {
        return new OnboardingStatisticsServiceImpl("APPNAME", TestUtils.objectMapper, errorNotifierServiceMock, initiativeStatRepositoryMock);
    }

    @Override
    protected List<String> getUseCases() {
        return Stream.of(
                        OnboardingOutcomeDTOFaker.mockInstance(0, "INITIATIVEID1"),
                        OnboardingOutcomeDTOFaker.mockInstance(1, "INITIATIVEID1"),
                        OnboardingOutcomeDTOFaker.mockInstance(2, "INITIATIVEID1"),
                        OnboardingOutcomeDTOFaker.mockInstanceBuilder(3, "INITIATIVEID1")
                                .status("ONBOARDING_KO")
                                .build(),
                        OnboardingOutcomeDTOFaker.mockInstance(4, "INITIATIVEID2"),
                        OnboardingOutcomeDTOFaker.mockInstanceBuilder(5, "INITIATIVEID2")
                                .status("ONBOARDING_KO")
                                .build(),
                        OnboardingOutcomeDTOFaker.mockInstance(6, "INITIATIVEID2"),
                        OnboardingOutcomeDTOFaker.mockInstance(7, "INITIATIVEID2")
                )
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    @Override
    protected void verifyResults(int partition0LastCommittedOffset, int partition1LastCommittedOffset) {
        Mockito.verify(initiativeStatRepositoryMock).retrieveOnboardingOutcomeCommittedOffset("INITIATIVEID1", 0);
        Mockito.verify(initiativeStatRepositoryMock).retrieveOnboardingOutcomeCommittedOffset("INITIATIVEID1", 1);
        Mockito.verify(initiativeStatRepositoryMock).retrieveOnboardingOutcomeCommittedOffset("INITIATIVEID1", 3);
        Mockito.verify(initiativeStatRepositoryMock).retrieveOnboardingOutcomeCommittedOffset("INITIATIVEID2", 0);
        Mockito.verify(initiativeStatRepositoryMock).retrieveOnboardingOutcomeCommittedOffset("INITIATIVEID2", 1);
        Mockito.verify(initiativeStatRepositoryMock).retrieveOnboardingOutcomeCommittedOffset("INITIATIVEID2", 3);

        Mockito.verify(initiativeStatRepositoryMock).updateOnboardingCount("INITIATIVEID1", 2, 0, partition0LastCommittedOffset);
        Mockito.verify(initiativeStatRepositoryMock).updateOnboardingCount("INITIATIVEID1", 1, 1, partition1LastCommittedOffset);
        Mockito.verify(initiativeStatRepositoryMock).updateOnboardingCount("INITIATIVEID1", 3, 3, EXPECTED_PARTITION3_OFFSET);

        Mockito.verify(initiativeStatRepositoryMock).updateOnboardingCount("INITIATIVEID2", 2, 0, partition0LastCommittedOffset);
        Mockito.verify(initiativeStatRepositoryMock).updateOnboardingCount("INITIATIVEID2", 1, 1, partition1LastCommittedOffset);
        Mockito.verify(initiativeStatRepositoryMock).updateOnboardingCount("INITIATIVEID2", 3, 3, EXPECTED_PARTITION3_OFFSET);

        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 0), new OffsetAndMetadata(partition0LastCommittedOffset))), Mockito.notNull());
        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 1), new OffsetAndMetadata(partition1LastCommittedOffset))), Mockito.isNull()); // no error messages in this partition, so no commit callback
        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 3), new OffsetAndMetadata(EXPECTED_PARTITION3_OFFSET))), Mockito.isNull());

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock, initiativeStatRepositoryMock, consumerMock);
    }
}
