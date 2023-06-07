package it.gov.pagopa.initiative.statistics.service.merchant.counters.notification;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationServiceTest;
import it.gov.pagopa.initiative.statistics.service.StatisticsErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.test.fakers.RewardNotificationDTOFaker;
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
class MerchantNotificationStatisticsServiceTest extends BaseStatisticsEvaluationServiceTest {

    public static final String MERCHANTID = "MERCHANTID";
    @Mock private StatisticsErrorNotifierService statisticsErrorNotifierServiceMock;
    @Mock private MerchantInitiativeCountersRepository merchantInitiativeCountersRepositoryMock;
    @Mock private Consumer<?,?> consumerMock;

    @Test
    void test(){
        invokeService("APPNAME", consumerMock, 5, 7, statisticsErrorNotifierServiceMock);

        Mockito.verifyNoMoreInteractions(statisticsErrorNotifierServiceMock, merchantInitiativeCountersRepositoryMock, consumerMock);
    }

    @Override
    protected StatisticsEvaluationService getStatisticsEvaluationServiceImpl() {
        return new MerchantNotificationStatisticsServiceImpl("APPNAME", "MERCHANT_COUNTERS_UPDATE_FROM_REWARD_NOTIFICATION", TestUtils.objectMapper, statisticsErrorNotifierServiceMock, merchantInitiativeCountersRepositoryMock);
    }

    @Override
    protected List<String> getUseCases() {
        return Stream.of(
                        RewardNotificationDTOFaker.mockInstanceBuilder(0, "INITIATIVEID1")
                                .rewardCents(4100L)
                                .beneficiaryId(MERCHANTID)
                                .build(),
                        RewardNotificationDTOFaker.mockInstanceBuilder(1, "INITIATIVEID1")
                                .rewardCents(2250L)
                                .beneficiaryId(MERCHANTID)
                                .build(),
                        RewardNotificationDTOFaker.mockInstanceBuilder(2, "INITIATIVEID1")
                                .rewardCents(6350L)
                                .beneficiaryId(MERCHANTID)
                                .build(),
                        RewardNotificationDTOFaker.mockInstanceBuilder(3, "INITIATIVEID1")
                                .rewardCents(0L)
                                .beneficiaryId(MERCHANTID)
                                .status("ONBOARDING_KO")
                                .build(),
                        RewardNotificationDTOFaker.mockInstanceBuilder(4, "INITIATIVEID2")
                                .rewardCents(6130L)
                                .beneficiaryId(MERCHANTID)
                                .build(),
                        RewardNotificationDTOFaker.mockInstanceBuilder(5, "INITIATIVEID2")
                                .rewardCents(8250L)
                                .beneficiaryId(MERCHANTID)
                                .status("ONBOARDING_KO")
                                .build(),
                        RewardNotificationDTOFaker.mockInstanceBuilder(6, "INITIATIVEID2")
                                .rewardCents(14380L)
                                .beneficiaryId(MERCHANTID)
                                .build(),
                        RewardNotificationDTOFaker.mockInstanceBuilder(7, "INITIATIVEID2")
                                .rewardCents(0L)
                                .beneficiaryId(MERCHANTID)
                                .build()
                )
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    @Override
    protected void verifyResults(int partition0LastCommittedOffset, int partition1LastCommittedOffset) {
        Mockito.verify(statisticsErrorNotifierServiceMock, Mockito.times(expectedErrorNotification)).notifyMerchantCountersRewardNotification(Mockito.any()
                , Mockito.argThat(description -> description.startsWith("[INITIATIVE_STATISTICS_EVALUATION][MERCHANT_COUNTERS_UPDATE_FROM_REWARD_NOTIFICATION] Unexpected json: "))
                , Mockito.eq(false), Mockito.any());

        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersNotificationCommittedOffset(buildCounterId("INITIATIVEID1"), MERCHANTID, "INITIATIVEID1", 0);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersNotificationCommittedOffset(buildCounterId("INITIATIVEID1"), MERCHANTID, "INITIATIVEID1", 1);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersNotificationCommittedOffset(buildCounterId("INITIATIVEID1"), MERCHANTID, "INITIATIVEID1", 3);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersNotificationCommittedOffset(buildCounterId("INITIATIVEID2"), MERCHANTID, "INITIATIVEID2", 0);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersNotificationCommittedOffset(buildCounterId("INITIATIVEID2"), MERCHANTID, "INITIATIVEID2", 1);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersNotificationCommittedOffset(buildCounterId("INITIATIVEID2"), MERCHANTID, "INITIATIVEID2", 3);

        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromRewardNotification(buildCounterId("INITIATIVEID1"), 10450L, 2L, 0, partition0LastCommittedOffset);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromRewardNotification(buildCounterId("INITIATIVEID1"), 2250L, 1L, 1, partition1LastCommittedOffset);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromRewardNotification(buildCounterId("INITIATIVEID1"), 12700L, 3L, 3, EXPECTED_PARTITION3_OFFSET);

        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromRewardNotification(buildCounterId("INITIATIVEID2"), 20510L, 2L, 0, partition0LastCommittedOffset);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromRewardNotification(buildCounterId("INITIATIVEID2"), 8250L,  1L, 1, partition1LastCommittedOffset);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromRewardNotification(buildCounterId("INITIATIVEID2"), 28760L,  3L, 3, EXPECTED_PARTITION3_OFFSET);

        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 0), new OffsetAndMetadata(partition0LastCommittedOffset+1))), Mockito.notNull());
        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 1), new OffsetAndMetadata(partition1LastCommittedOffset+1))), Mockito.isNull()); // no error messages in this partition, so no commit callback
        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 3), new OffsetAndMetadata(EXPECTED_PARTITION3_OFFSET+1))), Mockito.isNull());

        Mockito.verifyNoMoreInteractions(statisticsErrorNotifierServiceMock, merchantInitiativeCountersRepositoryMock, consumerMock);

        // TODO test reward < 0
    }

    private String buildCounterId(String initiativeId) {
        return "%s_%s".formatted(MERCHANTID, initiativeId);
    }
}
