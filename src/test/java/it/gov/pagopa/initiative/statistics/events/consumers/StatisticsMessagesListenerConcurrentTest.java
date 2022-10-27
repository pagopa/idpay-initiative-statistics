package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.test.utils.TestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

class StatisticsMessagesListenerConcurrentTest extends BaseIntegrationTest {

    public static final String INITIATIVEID = "INITIATIVEID";

    @AfterEach
    void clearData() {
        initiativeStatRepository.deleteById(INITIATIVEID);
    }

    @Test
    void test() {
        int validMsgs = 10000;
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
            publishIntoEmbeddedKafka(topicOnboardingOutcome, null, null, p.getKey());
            publishIntoEmbeddedKafka(topicTransactionEvaluation, null, p.getValue().getKey(), p.getValue().getValue());
        });
        long timePublishingEnd = System.currentTimeMillis();

        Assertions.assertEquals(validMsgs, waitForCounterResult(INITIATIVEID, "ORGANIZATIONID_"+INITIATIVEID, InitiativeStatistics::getOnboardedCitizenCount, validMsgs, maxWaitingMs));
        Assertions.assertEquals(validMsgs * 100, waitForCounterResult(INITIATIVEID, "ORGANIZATIONID_"+INITIATIVEID, InitiativeStatistics::getAccruedRewardsCents, validMsgs, maxWaitingMs));
        long timeCounterUpdated = System.currentTimeMillis();

        verifyPartitionOffsetStored(validMsgs, INITIATIVEID, InitiativeStatistics::getOnboardingOutcomeCommittedOffsets, true);
        verifyPartitionOffsetStored(validMsgs, INITIATIVEID, InitiativeStatistics::getTransactionEvaluationCommittedOffsets, false);

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
}
