package it.gov.pagopa.initiative.statistics.repository;

import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@DataMongoTest
@TestPropertySource(
        properties = {
                "de.flapdoodle.mongodb.embedded.version=4.2.24"
        }
)
class InitiativeStatAtomicOpsRepositoryTest {

    private static final String INITIATIVE_ID = "INITIATIVEID";
    private static final String ORGANIZATION_ID = "ORGANIZATIONID";

    @Autowired
    private InitiativeStatRepository repository;

    @AfterEach
    void clearData() {
        repository.deleteById(INITIATIVE_ID);
    }

    @Test
    void testRetrieveOnboardingOutcomeCommittedOffset() {
        // When record does not exist
        long result = repository.retrieveOnboardingOutcomeCommittedOffset(
                INITIATIVE_ID, null, 0);

        Assertions.assertEquals(-1L, result);

        InitiativeStatistics entity =
                repository.findById(INITIATIVE_ID).orElse(null);

        Assertions.assertNotNull(entity);
        Assertions.assertEquals(
                List.of(new CommittedOffset(0, -1)),
                entity.getOnboardingOutcomeCommittedOffsets());
        Assertions.assertNull(entity.getOrganizationId());

        // When record exists and organizationId is provided
        long result2 = repository.retrieveOnboardingOutcomeCommittedOffset(
                INITIATIVE_ID, ORGANIZATION_ID, 1);

        Assertions.assertEquals(-1L, result2);

        InitiativeStatistics entity2 =
                repository.findById(INITIATIVE_ID).orElse(null);

        Assertions.assertNotNull(entity2);
        Assertions.assertEquals(
                List.of(
                        new CommittedOffset(0, -1),
                        new CommittedOffset(1, -1)
                ),
                entity2.getOnboardingOutcomeCommittedOffsets());
        Assertions.assertEquals(ORGANIZATION_ID, entity2.getOrganizationId());
    }

    @Test
    void testRetrieveTransactionEvaluationCommittedOffset() {
        long result = repository.retrieveTransactionEvaluationCommittedOffset(
                INITIATIVE_ID, ORGANIZATION_ID, 0);

        Assertions.assertEquals(-1L, result);

        InitiativeStatistics entity =
                repository.findById(INITIATIVE_ID).orElse(null);

        Assertions.assertNotNull(entity);
        Assertions.assertEquals(
                List.of(new CommittedOffset(0, -1)),
                entity.getTransactionEvaluationCommittedOffsets());
        Assertions.assertEquals(ORGANIZATION_ID, entity.getOrganizationId());
    }

    @Test
    void testUpdateOnboardingCount() {
        InitiativeStatistics entity = InitiativeStatistics.builder()
                .initiativeId(INITIATIVE_ID)
                .onboardedCitizenCount(10L)
                .onboardingOutcomeCommittedOffsets(
                        List.of(new CommittedOffset(0, -1)))
                .build();

        repository.save(entity);

        repository.updateOnboardingCount(INITIATIVE_ID, 5, 0, 5);

        InitiativeStatistics result =
                repository.findById(INITIATIVE_ID).orElse(null);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(15L, result.getOnboardedCitizenCount());
        Assertions.assertEquals(
                List.of(new CommittedOffset(0, 5)),
                result.getOnboardingOutcomeCommittedOffsets());
    }

    @Test
    void testUpdateAccruedRewards() {
        InitiativeStatistics entity = InitiativeStatistics.builder()
                .initiativeId(INITIATIVE_ID)
                .accruedRewardsCents(100L)
                .rewardedTrxs(10L)
                .transactionEvaluationCommittedOffsets(
                        List.of(new CommittedOffset(1, -1)))
                .build();

        repository.save(entity);

        repository.updateAccruedRewards(INITIATIVE_ID, 500L, 1L, 1, 10);

        InitiativeStatistics result =
                repository.findById(INITIATIVE_ID).orElse(null);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(600L, result.getAccruedRewardsCents());
        Assertions.assertEquals(11L, result.getRewardedTrxs());
        Assertions.assertEquals(
                List.of(new CommittedOffset(1, 10)),
                result.getTransactionEvaluationCommittedOffsets());
    }

    @Test
    void testUpdateOnboardingCountThrowsExceptionWhenPartitionMissing() {
        InitiativeStatistics entity = InitiativeStatistics.builder()
                .initiativeId(INITIATIVE_ID)
                .onboardedCitizenCount(10L)
                .build();

        repository.save(entity);

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> repository.updateOnboardingCount(
                        INITIATIVE_ID, 5, 0, 5)
        );

        Assertions.assertTrue(
                ex.getMessage().contains(
                        "Counter increase called on not existent initiativeId-topicPartition"
                )
        );
    }
}