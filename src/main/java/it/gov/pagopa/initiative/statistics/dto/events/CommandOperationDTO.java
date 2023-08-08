package it.gov.pagopa.initiative.statistics.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class CommandOperationDTO {
    private String entityId;
    private String operationType;
    private LocalDateTime operationTime;
}
