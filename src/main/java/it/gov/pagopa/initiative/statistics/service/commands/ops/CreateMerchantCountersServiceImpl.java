package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CreateMerchantCountersServiceImpl implements CreateMerchantCountersService {
    private final MerchantInitiativeCountersRepository merchantInitiativeCountersRepository;

    public CreateMerchantCountersServiceImpl(MerchantInitiativeCountersRepository merchantInitiativeCountersRepository) {
        this.merchantInitiativeCountersRepository = merchantInitiativeCountersRepository;
    }
    @Override
    public void execute(String entityId) {
        if(StringUtils.isEmpty(entityId)){
            return;
        }
        String[] entity = StringUtils.split(entityId, "-");
        String initiativeId = entity[0];
        String merchantId = entity[1];
        String counterId = MerchantInitiativeCounters.buildId(merchantId, initiativeId);

        Optional<MerchantInitiativeCounters> result = merchantInitiativeCountersRepository.findById(counterId);

        if(result.isEmpty()){
            log.info("Initializing counters for merchant {} on initiative {}", merchantId, initiativeId);
            MerchantInitiativeCounters merchantStatistics = new MerchantInitiativeCounters(merchantId, initiativeId);
            merchantInitiativeCountersRepository.save(merchantStatistics);
            log.info("Initialized counters for merchant {} on initiative {}", merchantId, initiativeId);
        }
    }
}
