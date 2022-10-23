package it.gov.pagopa.initiative.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * InitiativeStatisticsDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InitiativeStatisticsDTO {

  private OffsetDateTime lastUpdatedDateTime;
  private Integer onboardedCitizenCount;
  private String accruedRewards;

}
