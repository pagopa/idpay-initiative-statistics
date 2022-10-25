package it.gov.pagopa.initiative.statistics.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * InitiativeStatisticsDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@Document("initiative_statistics")
public class InitiativeStatistics {

  @Id
  private String initiativeId;
  private String organizationId;
  private OffsetDateTime lastUpdatedDateTime;

  private Integer onboardedCitizenCount;
  private List<CommittedOffset> onboardingOutcomeCommittedOffsets;

  private Long accruedRewardsCents;
  private List<CommittedOffset> transactionEvaluationCommittedOffsets;

  @Data @AllArgsConstructor
  public static class CommittedOffset{
    private int partition;
    private long offset;
  }

}
