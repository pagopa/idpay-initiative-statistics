package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.dto.events.CommandOperationDTO;

public interface DeleteInitiativeService {
    void execute(CommandOperationDTO payload);
}
