package it.gov.pagopa.initiative.statistics.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

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
