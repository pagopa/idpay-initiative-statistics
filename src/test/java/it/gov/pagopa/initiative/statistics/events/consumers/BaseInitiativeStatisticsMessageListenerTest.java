package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseInitiativeStatisticsMessageListenerTest extends BaseStatisticsMessagesListenerTest<InitiativeStatistics> {

    @Autowired
    protected InitiativeStatRepository initiativeStatRepository;

    @Override
    protected InitiativeStatRepository getStatRepository() {
        return initiativeStatRepository;
    }

    @Override
    protected InitiativeStatistics buildStatisticInstance(String initiativeId) {
        return InitiativeStatistics.builder()
                .initiativeId(initiativeId)
                .build();
    }

    @Override
    protected String buildCounterId(String initiativeId) {
        return initiativeId;
    }


    @Override
    protected void checkAndEmptyTimestampFields(InitiativeStatistics retrieved) {
        Assertions.assertNotNull(retrieved.getLastUpdatedDateTime());

        retrieved.setLastUpdatedDateTime(null);
    }
}
