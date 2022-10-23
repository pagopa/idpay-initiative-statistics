package it.gov.pagopa.initiative.statistics.repository;

import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InitiativeStatRepository extends MongoRepository<InitiativeStatistics, String> {
}
