package it.gov.pagopa.initiative.statistics.service;

import it.gov.pagopa.initiative.statistics.exception.StatisticsNotFoundException;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
@Slf4j
public class InitiativeStatServiceImpl implements InitiativeStatService {

    private final InitiativeStatRepository initiativeStatRepository;

    public InitiativeStatServiceImpl(InitiativeStatRepository initiativeStatRepository) {
        this.initiativeStatRepository = initiativeStatRepository;
    }

    @Override
    public InitiativeStatistics getStatistics(String organizationId, String initiativeId) {
        return initiativeStatRepository.findById(initiativeId)
                .filter(s -> Objects.equals(s.getOrganizationId(), organizationId))
                .orElseThrow(() -> new StatisticsNotFoundException(
                        "No stats found for initiative with id: %s and organization: %s".formatted(initiativeId, organizationId)));
    }

}
