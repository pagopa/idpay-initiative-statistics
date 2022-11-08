package it.gov.pagopa.initiative.statistics.repository;

import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

class InitiativeStatAtomicOpsRepositoryTest extends BaseIntegrationTest {

    private final String initiativeid = "INITIATIVEID";

    @Autowired
    private InitiativeStatRepository repository;

    @AfterEach
    void clearData(){
        repository.deleteById(initiativeid);
    }

    @Test
    void testRetrieveOnboardingOutcomeCommittedOffset(){
        InitiativeStatistics.CommittedOffset expectedPartition0 = new InitiativeStatistics.CommittedOffset(0, -1);
        InitiativeStatistics.CommittedOffset expectedPartition1 = new InitiativeStatistics.CommittedOffset(1, -1);

        // test when not exists not providing organizationId
        long result = repository.retrieveOnboardingOutcomeCommittedOffset(initiativeid, null, 0);
        Assertions.assertEquals(-1L, result);

        InitiativeStatistics entity = repository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(List.of(expectedPartition0), entity.getOnboardingOutcomeCommittedOffsets());
        Assertions.assertNull(entity.getOrganizationId());

        // test when exists providing organizationId
        long result2 = repository.retrieveOnboardingOutcomeCommittedOffset(initiativeid, "ORGANIZATIONID", 1);
        Assertions.assertEquals(-1L, result2);

        InitiativeStatistics entity2 = repository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(entity2);
        Assertions.assertEquals(List.of(expectedPartition0, expectedPartition1), entity2.getOnboardingOutcomeCommittedOffsets());
        Assertions.assertEquals("ORGANIZATIONID", entity2.getOrganizationId());

        // test when initiative and partition already exist, trying to change organization (it cannot be modified)
        InitiativeStatistics.CommittedOffset expectedPartition3 = new InitiativeStatistics.CommittedOffset(3, 50);
        entity2.setOnboardingOutcomeCommittedOffsets(List.of(expectedPartition3));
        repository.save(entity2);

        long result3 = repository.retrieveOnboardingOutcomeCommittedOffset(initiativeid, "ORGANIZATIONIDXXX", 3);
        Assertions.assertEquals(50, result3);

        InitiativeStatistics entity3 = repository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(entity3);
        Assertions.assertEquals(List.of(expectedPartition3), entity3.getOnboardingOutcomeCommittedOffsets());
        Assertions.assertEquals("ORGANIZATIONID", entity3.getOrganizationId());
    }

    @Test
    void testRetrieveTransactionEvaluationCommittedOffset(){
        InitiativeStatistics.CommittedOffset expectedPartition0 = new InitiativeStatistics.CommittedOffset(0, -1);
        InitiativeStatistics.CommittedOffset expectedPartition1 = new InitiativeStatistics.CommittedOffset(1, -1);

        // test when not exists not providing organizationId
        long result = repository.retrieveTransactionEvaluationCommittedOffset(initiativeid, null, 0);
        Assertions.assertEquals(-1L, result);

        InitiativeStatistics entity = repository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(List.of(expectedPartition0), entity.getTransactionEvaluationCommittedOffsets());

        // test when exists providing organizationId
        long result2 = repository.retrieveTransactionEvaluationCommittedOffset(initiativeid, "ORGANIZATIONID", 1);
        Assertions.assertEquals(-1L, result2);

        InitiativeStatistics entity2 = repository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(entity2);
        Assertions.assertEquals(List.of(expectedPartition0, expectedPartition1), entity2.getTransactionEvaluationCommittedOffsets());
        Assertions.assertEquals("ORGANIZATIONID", entity2.getOrganizationId());

        // test when initiative and partition already exist, trying to change organization (it cannot be modified)
        InitiativeStatistics.CommittedOffset expectedPartition3 = new InitiativeStatistics.CommittedOffset(3, 50);
        entity2.setTransactionEvaluationCommittedOffsets(List.of(expectedPartition3));
        repository.save(entity2);

        long result3 = repository.retrieveTransactionEvaluationCommittedOffset(initiativeid, null, 3);
        Assertions.assertEquals(50, result3);

        InitiativeStatistics entity3 = repository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(entity3);
        Assertions.assertEquals(List.of(expectedPartition3), entity3.getTransactionEvaluationCommittedOffsets());
        Assertions.assertEquals("ORGANIZATIONID", entity3.getOrganizationId());
    }

    @Test
    void testUpdateOnboardingCount(){
        // increasing when not initiative
        try{
            repository.updateOnboardingCount(initiativeid, 0, 0, 0);
        } catch (IllegalStateException e){
            Assertions.assertEquals(
                    "[INITIATIVE_STATISTICS_EVALUATION][INC_onboardedCitizenCount] Counter increase called on not existent initiativeId-topicPartition: INITIATIVEID 0",
                    e.getMessage());
        }

        // increasing when not partition
        InitiativeStatistics entity = InitiativeStatistics.builder().initiativeId(initiativeid).build();
        repository.save(entity);

        try{
            repository.updateOnboardingCount(initiativeid, 0, 0, 0);
        } catch (IllegalStateException e){
            Assertions.assertEquals(
                    "[INITIATIVE_STATISTICS_EVALUATION][INC_onboardedCitizenCount] Counter increase called on not existent initiativeId-topicPartition: INITIATIVEID 0",
                    e.getMessage());
        }

        // successfulUseCase
        entity.setOnboardedCitizenCount(10L);
        entity.setOnboardingOutcomeCommittedOffsets(List.of(new InitiativeStatistics.CommittedOffset(0, -1)));
        repository.save(entity);

        repository.updateOnboardingCount(initiativeid, 5, 0, 5);

        InitiativeStatistics result = repository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiativeid, result.getInitiativeId());
        Assertions.assertEquals(15, result.getOnboardedCitizenCount());
        Assertions.assertEquals(List.of(new InitiativeStatistics.CommittedOffset(0, 5)),
                result.getOnboardingOutcomeCommittedOffsets());
    }

    @Test
    void testUpdateAccruedReward(){
        // increasing when not initiative
        try{
            repository.updateAccruedRewards(initiativeid, BigDecimal.valueOf(0), 0L, 0, 0);
        } catch (IllegalStateException e){
            Assertions.assertEquals(
                    "[INITIATIVE_STATISTICS_EVALUATION][INC_rewardedTrxs][INC_accruedRewardsCents] Counter increase called on not existent initiativeId-topicPartition: INITIATIVEID 0",
                    e.getMessage());
        }

        // increasing when not partition
        InitiativeStatistics entity = InitiativeStatistics.builder().initiativeId(initiativeid).build();
        repository.save(entity);

        try{
            repository.updateAccruedRewards(initiativeid, BigDecimal.valueOf(0), 0L, 0, 0);
        } catch (IllegalStateException e){
            Assertions.assertEquals(
                    "[INITIATIVE_STATISTICS_EVALUATION][INC_rewardedTrxs][INC_accruedRewardsCents] Counter increase called on not existent initiativeId-topicPartition: INITIATIVEID 0",
                    e.getMessage());
        }

        // successfulUseCase
        entity.setAccruedRewardsCents(100L);
        entity.setRewardedTrxs(10L);
        entity.setTransactionEvaluationCommittedOffsets(List.of(new InitiativeStatistics.CommittedOffset(1, -1)));
        repository.save(entity);

        repository.updateAccruedRewards(initiativeid, BigDecimal.valueOf(5), 1L, 1, 10);

        InitiativeStatistics result = repository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiativeid, result.getInitiativeId());
        Assertions.assertEquals(600L, result.getAccruedRewardsCents());
        Assertions.assertEquals(11L, result.getRewardedTrxs());
        Assertions.assertEquals(List.of(new InitiativeStatistics.CommittedOffset(1, 10)),
                result.getTransactionEvaluationCommittedOffsets());
    }
}
