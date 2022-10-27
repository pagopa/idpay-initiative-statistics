package it.gov.pagopa.initiative.statistics.service;

import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;

public interface InitiativeStatService {

    InitiativeStatistics getStatistics(String organizationId, String initiativeId);

}
