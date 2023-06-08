package it.gov.pagopa.initiative.statistics.repository.merchant.counters;

import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MerchantInitiativeCountersRepository extends MongoRepository<MerchantInitiativeCounters, String>, MerchantInitiativeCountersOpsRepository {
}
