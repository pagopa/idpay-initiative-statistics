package it.gov.pagopa.initiative.statistics.repository.merchant.counters;

import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MerchantInitiativeCountersRepository extends MongoRepository<MerchantInitiativeCounters, String>, MerchantInitiativeCountersOpsRepository {
    List<MerchantInitiativeCounters> deleteByInitiativeId(String initiativeId);
}
