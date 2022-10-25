package it.gov.pagopa.initiative.statistics.service.trx;

import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationServiceTest;
import it.gov.pagopa.initiative.statistics.service.ErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.test.fakers.TransactionEvaluationDTOFaker;
import it.gov.pagopa.initiative.statistics.test.utils.TestUtils;
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
class TransactionEvaluationStatisticsServiceTest extends BaseStatisticsEvaluationServiceTest {

    @Mock
    private ErrorNotifierService errorNotifierServiceMock;
    @Mock private InitiativeStatRepository initiativeStatRepositoryMock;
    @Mock private Consumer<?,?> consumerMock;

    @Test
    void test(){
        invokeService("APPNAME", consumerMock, 5, 7, errorNotifierServiceMock);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock, initiativeStatRepositoryMock, consumerMock);
    }

    @Override
    protected StatisticsEvaluationService getStatisticsEvaluationServiceImpl() {
        return new TransactionEvaluationStatisticsServiceImpl("APPNAME", TestUtils.objectMapper, errorNotifierServiceMock, initiativeStatRepositoryMock);
    }

    @Override
    protected List<String> getUseCases() {
        return Stream.of(
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(0)
                                .rewards(Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(1.1))))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(1)
                                .rewards(Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(2.25))))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(2)
                                .rewards(Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(3))))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(3, "INITIATIVEID1")
                                .rewards(Collections.emptyMap())
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(4)
                                .rewards(Map.of("INITIATIVEID2", new Reward(BigDecimal.valueOf(1))))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(5, "INITIATIVEID2")
                                .rewards(Collections.emptyMap())
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(6)
                                .rewards(Map.of("INITIATIVEID2", new Reward(BigDecimal.valueOf(5.13))))
                                .build(),
                        TransactionEvaluationDTOFaker.mockInstanceBuilder(7)
                                .rewards(Map.of("INITIATIVEID2", new Reward(BigDecimal.valueOf(8.25))))
                                .build()
                )
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    @Override
    protected void verifyResults(int partition0LastCommittedOffset, int partition1LastCommittedOffset) {
        Mockito.verify(errorNotifierServiceMock, Mockito.times(expectedErrorNotification)).notifyTransactionEvaluation(Mockito.any()
                , Mockito.argThat(description -> description.startsWith("[INITIATIVE_STATISTICS_EVALUATION][TRANSACTION_EVALUATION] Unexpected json: "))
                , Mockito.eq(false), Mockito.any());

        Mockito.verify(initiativeStatRepositoryMock).retrieveTransactionEvaluationCommittedOffset("INITIATIVEID1", 0);
        Mockito.verify(initiativeStatRepositoryMock).retrieveTransactionEvaluationCommittedOffset("INITIATIVEID1", 1);
        Mockito.verify(initiativeStatRepositoryMock).retrieveTransactionEvaluationCommittedOffset("INITIATIVEID1", 3);
        Mockito.verify(initiativeStatRepositoryMock).retrieveTransactionEvaluationCommittedOffset("INITIATIVEID2", 0);
        Mockito.verify(initiativeStatRepositoryMock).retrieveTransactionEvaluationCommittedOffset("INITIATIVEID2", 1);
        Mockito.verify(initiativeStatRepositoryMock).retrieveTransactionEvaluationCommittedOffset("INITIATIVEID2", 3);

        Mockito.verify(initiativeStatRepositoryMock).updateAccruedRewards("INITIATIVEID1", BigDecimal.valueOf(4.1), 0, partition0LastCommittedOffset);
        Mockito.verify(initiativeStatRepositoryMock).updateAccruedRewards("INITIATIVEID1", BigDecimal.valueOf(2.25), 1, partition1LastCommittedOffset);
        Mockito.verify(initiativeStatRepositoryMock).updateAccruedRewards("INITIATIVEID1", BigDecimal.valueOf(6.35), 3, EXPECTED_PARTITION3_OFFSET);

        Mockito.verify(initiativeStatRepositoryMock).updateAccruedRewards("INITIATIVEID2", BigDecimal.valueOf(6.13), 0, partition0LastCommittedOffset);
        Mockito.verify(initiativeStatRepositoryMock).updateAccruedRewards("INITIATIVEID2", BigDecimal.valueOf(8.25), 1, partition1LastCommittedOffset);
        Mockito.verify(initiativeStatRepositoryMock).updateAccruedRewards("INITIATIVEID2", BigDecimal.valueOf(14.38), 3, EXPECTED_PARTITION3_OFFSET);

        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 0), new OffsetAndMetadata(partition0LastCommittedOffset))), Mockito.notNull());
        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 1), new OffsetAndMetadata(partition1LastCommittedOffset))), Mockito.isNull()); // no error messages in this partition, so no commit callback
        Mockito.verify(consumerMock).commitAsync(Mockito.eq(Map.of(new TopicPartition(TOPIC_NAME, 3), new OffsetAndMetadata(EXPECTED_PARTITION3_OFFSET))), Mockito.isNull());

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock, initiativeStatRepositoryMock, consumerMock);
    }
}
