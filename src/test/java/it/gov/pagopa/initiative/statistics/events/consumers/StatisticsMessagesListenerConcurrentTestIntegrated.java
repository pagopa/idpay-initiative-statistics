package it.gov.pagopa.initiative.statistics.events.consumers;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
class StatisticsMessagesListenerConcurrentTestIntegrated extends StatisticsMessagesListenerConcurrentTest {
}
