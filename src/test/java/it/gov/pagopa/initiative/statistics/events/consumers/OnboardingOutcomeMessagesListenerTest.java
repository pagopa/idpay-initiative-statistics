package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

class OnboardingOutcomeMessagesListenerTest extends BaseIntegrationTest {
    @Test
    void test(){
        publishIntoEmbeddedKafka(topicOnboardingOutcome, null, null, "PROVA");
    }
}
