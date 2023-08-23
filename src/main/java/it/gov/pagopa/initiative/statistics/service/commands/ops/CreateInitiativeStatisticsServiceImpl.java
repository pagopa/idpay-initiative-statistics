package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CreateInitiativeStatisticsServiceImpl implements CreateInitiativeStatisticsService{
    private final InitiativeStatRepository initiativeStatRepository;

    public CreateInitiativeStatisticsServiceImpl(InitiativeStatRepository initiativeStatRepository, MerchantInitiativeCountersRepository merchantInitiativeCountersRepository, AuditUtilities auditUtilities) {
        this.initiativeStatRepository = initiativeStatRepository;
    }
    @Override
    public void execute(String initiativeId, String organizationId) {
        Optional<InitiativeStatistics> result = initiativeStatRepository.findById(initiativeId);

        if(result.isEmpty()){
            InitiativeStatistics initiativeStatistics = InitiativeStatistics.builder()
                    .initiativeId(initiativeId)
                    .organizationId(organizationId)
                    .build();
            initiativeStatRepository.save(initiativeStatistics);
            log.info("Initialized statistics for initiative {}", initiativeId);
        }
    }
}
