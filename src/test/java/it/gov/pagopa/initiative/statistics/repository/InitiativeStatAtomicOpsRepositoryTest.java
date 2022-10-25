package it.gov.pagopa.initiative.statistics.repository;

import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InitiativeStatAtomicOpsRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private InitiativeStatRepository repository;

    @Test
    void testRetrieveOnboardingOutcomeCommittedOffset(){
        // test when not exists not providing organizationId
        long result = repository.retrieveOnboardingOutcomeCommittedOffset("INITIATIVEID", null, 0);
        Assertions.assertEquals(-1L, result);
        // TODO assert retrieving entity

        // test when exists providing organizationId
        long result2 = repository.retrieveOnboardingOutcomeCommittedOffset("INITIATIVEID", "ORGANIZATIONID", 0);
        Assertions.assertEquals(-1L, result2);
        // TODO assert retrieving entity

        // TODO test with already stored partition
    }
}
