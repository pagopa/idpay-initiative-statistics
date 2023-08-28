package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CreateInitiativeStatisticsServiceImpl implements CreateInitiativeStatisticsService{
    private final InitiativeStatRepository initiativeStatRepository;

    public CreateInitiativeStatisticsServiceImpl(InitiativeStatRepository initiativeStatRepository) {
        this.initiativeStatRepository = initiativeStatRepository;
    }
    @Override
    public void execute(String entityId) {
        if(StringUtils.isEmpty(entityId)){
            return;
        }
        String[] entity = StringUtils.split(entityId, "_");
        String initiativeId = entity[0];
        String organizationId = entity[1];
        Optional<InitiativeStatistics> result = initiativeStatRepository.findById(initiativeId);

        if(result.isEmpty()){
            log.info("Initializing statistics for initiative {}", initiativeId);
            InitiativeStatistics initiativeStatistics = InitiativeStatistics.builder()
                    .initiativeId(initiativeId)
                    .organizationId(organizationId)
                    .build();
            initiativeStatRepository.save(initiativeStatistics);
            log.info("Initialized statistics for initiative {}", initiativeId);
        }
    }
}
