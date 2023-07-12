package it.gov.pagopa.initiative.statistics.dto.events;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingOutcomeDTO {
    @NotEmpty
    private String userId;
    @NotEmpty
    private String initiativeId;
    private String organizationId;
    @NotEmpty
    private String status;
}
