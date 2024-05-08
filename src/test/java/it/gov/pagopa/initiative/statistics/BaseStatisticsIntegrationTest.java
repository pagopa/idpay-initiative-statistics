package it.gov.pagopa.initiative.statistics;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.dto.events.OnboardingOutcomeDTO;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.dto.events.RewardNotificationDTO;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.test.fakers.OnboardingOutcomeDTOFaker;
import it.gov.pagopa.initiative.statistics.test.fakers.RewardNotificationDTOFaker;
import it.gov.pagopa.initiative.statistics.test.fakers.TransactionEvaluationDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

public abstract class BaseStatisticsIntegrationTest extends BaseIntegrationTest{

    protected List<OnboardingOutcomeDTO> buildValidOnboardinOutcomesEntities(int bias, int size, String initiativeid) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> buildValidOnboardinOutcomeEntity(i, initiativeid))
                .toList();
    }
    protected OnboardingOutcomeDTO buildValidOnboardinOutcomeEntity(int bias, String initiativeid){
        return OnboardingOutcomeDTOFaker.mockInstance(bias, initiativeid);
    }

    protected List<TransactionEvaluationDTO> buildValidTransactionEvaluationEntities(int bias, int size, String initiativeid) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> buildValidTransactionEvaluationEntity(i, initiativeid))
                .toList();
    }
    protected TransactionEvaluationDTO buildValidTransactionEvaluationEntity(int bias, String initiativeid) {
        return TransactionEvaluationDTOFaker.mockInstanceBuilder(bias)
                .rewards(Map.of(initiativeid, new Reward(initiativeid, "ORGANIZATIONID_%s".formatted(initiativeid), 100L, bias%3==0, bias%6==0)))
                .build();
    }
    protected List<RewardNotificationDTO> buildValidRewardNotificationEntities(int bias, int size, String initiativeid, boolean merchant) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> buildValidRewardNotificationEntity(i, initiativeid, merchant))
                .toList();
    }
    protected RewardNotificationDTO buildValidRewardNotificationEntity(int bias, String initiativeid, boolean merchant) {
        return RewardNotificationDTOFaker.mockInstanceBuilder(bias, initiativeid, merchant)
                .rewardCents(100L)
                .build();
    }
    protected int getExpectedTrxsCount(int validMsgs) {
        int zeroBasedFix = validMsgs % 3 == 0 ? 0 : 1; // because 0 based, we have to add 1, but if using a multiple of 3, we have to remove one, compensating the 0 based bias
        return validMsgs
                - (validMsgs / 6 * 2 + zeroBasedFix) // each %6 will be a complete refund
                - (validMsgs / 3 - validMsgs / 6 + zeroBasedFix); // each %3 will be a refund
    }

    protected <T> long waitForCounterResult(String initiativeId, String organizationId, Function<T, Long> getterCounter, long expectedCounterValue, long maxWaitingMs, MongoRepository<T, String> statisticRepository) {
        int millisAttemptDelay = 500;
        int maxAttempts = (int) maxWaitingMs / millisAttemptDelay;

        long[] countSaved = {0};
        TestUtils.waitFor(() -> (countSaved[0] = statisticRepository.findById(buildCounterId(initiativeId))
                        .filter(r-> {
                            if (r instanceof InitiativeStatistics initiativeStatistics) {
                                Assertions.assertEquals(organizationId, initiativeStatistics.getOrganizationId());
                            }
                            return true;
                        })
                        .map(getterCounter).orElse(-1L)) >= expectedCounterValue
                , () -> "Expected %d counter value for initiative %s, read %d".formatted(expectedCounterValue, initiativeId, countSaved[0])
                , maxAttempts, millisAttemptDelay);
        return countSaved[0];
    }

    protected <T> long verifyPartitionOffsetStored(long expectOffsetSum, String initiativeId, Function<T, List<CommittedOffset>> getterStatisticsCommittedOffsets, boolean assertEquals, MongoRepository<T, String> statisticRepository) {
        T result = statisticRepository.findById(buildCounterId(initiativeId)).orElse(null);
        Assertions.assertNotNull(result);

        // -2 because offset start from 0 and we are using 2 partition for test
        long expectedOffsetSum0Based = expectOffsetSum - 2;
        long sum = getterStatisticsCommittedOffsets.apply(result).stream().mapToLong(CommittedOffset::getOffset).sum();

        if (assertEquals) {
            Assertions.assertEquals(expectedOffsetSum0Based, sum);
        } else {
            Assertions.assertTrue(expectedOffsetSum0Based>=sum, "Expected at least %d obtained %d".formatted(expectedOffsetSum0Based, sum));
        }

        return sum + 2;
    }

    protected abstract String buildCounterId(String initiativeId);

}
