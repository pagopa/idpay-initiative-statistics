package it.gov.pagopa.initiative.statistics.repository;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
class InitiativeStatAtomicOpsRepositoryTestIntegrated extends InitiativeStatAtomicOpsRepositoryTest {
}
