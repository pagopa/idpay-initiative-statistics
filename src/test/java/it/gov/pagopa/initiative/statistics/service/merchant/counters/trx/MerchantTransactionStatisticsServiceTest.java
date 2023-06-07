package it.gov.pagopa.initiative.statistics.service.merchant.counters.trx;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationServiceTest;
import it.gov.pagopa.initiative.statistics.service.StatisticsErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.test.fakers.TransactionEvaluationDTOFaker;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MerchantTransactionStatisticsServiceTest extends BaseStatisticsEvaluationServiceTest {

    private static final String MERCHANTID = "MERCHANTID";

    @Mock
    private StatisticsErrorNotifierService statisticsErrorNotifierServiceMock;
    @Mock private MerchantInitiativeCountersRepository merchantInitiativeCountersRepositoryMock;
    @Mock private Consumer<?,?> consumerMock;

    @Test
    void test(){
        invokeService("APPNAME", consumerMock, 5, 7, statisticsErrorNotifierServiceMock);

        Mockito.verifyNoMoreInteractions(statisticsErrorNotifierServiceMock, merchantInitiativeCountersRepositoryMock, consumerMock);
    }

    @Override
    protected StatisticsEvaluationService getStatisticsEvaluationServiceImpl() {
        return new MerchantTransactionStatisticsServiceImpl("APPNAME", "MERCHANT_TRANSACTION_GROUP", TestUtils.objectMapper, statisticsErrorNotifierServiceMock, merchantInitiativeCountersRepositoryMock);
    }

    @Override
    protected List<String> getUseCases() {
        return Stream.of(
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(0)
                                .merchantId(MERCHANTID)
                                .rewards(Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATIONID1", BigDecimal.valueOf(1.1))))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(1)
                                .merchantId(MERCHANTID)
                                .rewards(Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATIONID1", BigDecimal.valueOf(2.25), true, false)))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(2)
                                .merchantId(MERCHANTID)
                                .rewards(Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATIONID1", BigDecimal.valueOf(3), true, true)))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(3, "INITIATIVEID1")
                                .merchantId(MERCHANTID)
                                .rewards(Collections.emptyMap())
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(4)
                                .merchantId(MERCHANTID)
                                .rewards(Map.of("INITIATIVEID2", new Reward("INITIATIVEID2", "ORGANIZATIONID2", BigDecimal.valueOf(1))))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(5, "INITIATIVEID2")
                                .merchantId(MERCHANTID)
                                .rewards(Collections.emptyMap())
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(6)
                                .merchantId(MERCHANTID)
                                .rewards(Map.of("INITIATIVEID2", new Reward("INITIATIVEID2", "ORGANIZATIONID2", BigDecimal.valueOf(5.13), true, false)))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(7)
                                .merchantId(MERCHANTID)
                                .rewards(Map.of("INITIATIVEID2", new Reward("INITIATIVEID2", "ORGANIZATIONID2", BigDecimal.valueOf(8.25), true, true)))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(8)
                                .merchantId(MERCHANTID)
                                .rewards(Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATIONID1", BigDecimal.ZERO, false, false)))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(9)
                                .merchantId(MERCHANTID)
                                .rewards(Map.of("INITIATIVEID2", new Reward("INITIATIVEID2", "ORGANIZATIONID2", BigDecimal.ZERO, false, false)))
                                .build()
                )
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    @Override
    protected void verifyResults(int partition0LastCommittedOffset, int partition1LastCommittedOffset) {
        Mockito.verify(statisticsErrorNotifierServiceMock, Mockito.times(expectedErrorNotification)).notifyMerchantCountersTransaction(Mockito.any()
                , Mockito.argThat(description -> description.startsWith("[INITIATIVE_STATISTICS_EVALUATION][MERCHANT_COUNTERS_UPDATE_FROM_TRANSACTION] Unexpected json: "))
                , Mockito.eq(false), Mockito.any());

        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersTransactionCommittedOffset(buildCounterId("INITIATIVEID1"), MERCHANTID, "INITIATIVEID1", 0);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersTransactionCommittedOffset(buildCounterId("INITIATIVEID1"), MERCHANTID, "INITIATIVEID1", 1);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersTransactionCommittedOffset(buildCounterId("INITIATIVEID1"), MERCHANTID, "INITIATIVEID1", 3);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersTransactionCommittedOffset(buildCounterId("INITIATIVEID2"), MERCHANTID, "INITIATIVEID2", 0);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersTransactionCommittedOffset(buildCounterId("INITIATIVEID2"), MERCHANTID, "INITIATIVEID2", 1);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).retrieveMerchantCountersTransactionCommittedOffset(buildCounterId("INITIATIVEID2"), MERCHANTID, "INITIATIVEID2", 3);

        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromTransaction(buildCounterId("INITIATIVEID1"), BigDecimal.valueOf(4.1), 0L, 0, partition0LastCommittedOffset);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromTransaction(buildCounterId("INITIATIVEID1"), BigDecimal.valueOf(2.25), 0L, 1, partition1LastCommittedOffset);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromTransaction(buildCounterId("INITIATIVEID1"), BigDecimal.valueOf(6.35), 0L, 3, EXPECTED_PARTITION3_OFFSET);

        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromTransaction(buildCounterId("INITIATIVEID2"), BigDecimal.valueOf(6.13), 1L,0, partition0LastCommittedOffset);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromTransaction(buildCounterId("INITIATIVEID2"), BigDecimal.valueOf(8.25),  -1L,1, partition1LastCommittedOffset);
        Mockito.verify(merchantInitiativeCountersRepositoryMock).updateCountersFromTransaction(buildCounterId("INITIATIVEID2"), BigDecimal.valueOf(14.38),  0L, 3, EXPECTED_PARTITION3_OFFSET);

        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 0), new OffsetAndMetadata(partition0LastCommittedOffset+1))), Mockito.notNull());
        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 1), new OffsetAndMetadata(partition1LastCommittedOffset+1))), Mockito.isNull()); // no error messages in this partition, so no commit callback
        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 3), new OffsetAndMetadata(EXPECTED_PARTITION3_OFFSET+1))), Mockito.isNull());

        Mockito.verifyNoMoreInteractions(statisticsErrorNotifierServiceMock, merchantInitiativeCountersRepositoryMock, consumerMock);

        // TODO test null merchantId
    }

    private String buildCounterId(String initiativeId) {
        return MerchantInitiativeCounters.buildId(MERCHANTID, initiativeId);
    }
}
