package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.BaseStatisticsIntegrationTest;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.stream.IntStream;

@DirtiesContext
class StatisticsMessagesListenerConcurrentTest extends BaseStatisticsIntegrationTest {

    public static final String INITIATIVEID = "INITIATIVEID";

    @Autowired
    protected InitiativeStatRepository initiativeStatRepository;

    @AfterEach
    void clearData() {
        initiativeStatRepository.deleteById(INITIATIVEID);
    }

    @Test
    void test() {
        int validMsgs = 1000;
        long maxWaitingMs = 30000;

        List<Pair<String, Pair<String, String>>> msgs = IntStream.range(0, validMsgs).mapToObj(i -> {
            TransactionEvaluationDTO trxEvaluation = buildValidTransactionEvaluationEntity(i, INITIATIVEID);
            return Pair.of(
                    TestUtils.jsonSerializer(buildValidOnboardinOutcomeEntity(i, INITIATIVEID)),
                    Pair.of(trxEvaluation.getUserId(), TestUtils.jsonSerializer(trxEvaluation))
            );
        }).toList();

        long timePublishStart = System.currentTimeMillis();
        msgs.parallelStream().forEach(p -> {
            kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicOnboardingOutcome, null, null, p.getKey());
            kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicTransactionEvaluation, null, p.getValue().getKey(), p.getValue().getValue());
        });
        long timePublishingEnd = System.currentTimeMillis();

        Assertions.assertEquals(validMsgs, waitForCounterResult(INITIATIVEID, "ORGANIZATIONID_"+INITIATIVEID, InitiativeStatistics::getOnboardedCitizenCount, validMsgs, maxWaitingMs, initiativeStatRepository));
        Assertions.assertEquals(validMsgs * 100, waitForCounterResult(INITIATIVEID, "ORGANIZATIONID_"+INITIATIVEID, InitiativeStatistics::getAccruedRewardsCents, validMsgs * 100, maxWaitingMs, initiativeStatRepository));
        long timeCounterUpdated = System.currentTimeMillis();
        Assertions.assertEquals(getExpectedTrxsCount(validMsgs), initiativeStatRepository.findById(buildCounterId(INITIATIVEID)).map(InitiativeStatistics::getRewardedTrxs).orElse(null));

        verifyPartitionOffsetStored(validMsgs, INITIATIVEID, InitiativeStatistics::getOnboardingOutcomeCommittedOffsets, true, initiativeStatRepository);
        verifyPartitionOffsetStored(validMsgs, INITIATIVEID, InitiativeStatistics::getTransactionEvaluationCommittedOffsets, false, initiativeStatRepository);

        System.out.printf("""
                        ************************
                        Time spent to send %d onboarding outcome, %d trx evaluation messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                msgs.size(),
                msgs.size(),
                timePublishingEnd - timePublishStart,
                timeCounterUpdated - timePublishingEnd,
                timeCounterUpdated - timePublishStart
        );
    }

    @Override
    protected String buildCounterId(String initiativeId) {
        return initiativeId;
    }
}
