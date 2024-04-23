package it.gov.pagopa.initiative.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * InitiativeStatisticsDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InitiativeStatisticsDTO {

  private LocalDateTime lastUpdatedDateTime;
  private long onboardedCitizenCount;
  private long rewardedTrxs;
  private Long accruedRewardsCents;

}
