package it.gov.pagopa.initiative.statistics.service.commands.ops;

public interface CreateInitiativeStatisticsService {
    void execute(String initiativeId, String organizationId);
}
