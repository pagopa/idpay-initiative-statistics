package it.gov.pagopa.initiative.statistics.service;

import it.gov.pagopa.initiative.statistics.exception.InitiativeStatException;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Objects;


@Service
@Slf4j
public class InitiativeStatServiceImpl implements InitiativeStatService {

    @Autowired
    private InitiativeStatRepository initiativeStatRepository;

    @Override
    public InitiativeStatistics getStatistics(String organizationId, String initiativeId) {
        return initiativeStatRepository.findById(initiativeId)
                .filter(s -> Objects.equals(s.getOrganizationId(), organizationId))
                .orElseThrow(() -> new InitiativeStatException(HttpStatus.NOT_FOUND.name(),
                        MessageFormat.format("No stats found for initiative with id: {0} and organization: {1}", initiativeId, organizationId),
                        HttpStatus.NOT_FOUND));
    }

}
