package it.gov.pagopa.initiative.statistics.model;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
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
@FieldNameConstants
public class InitiativeStatistics {

  @Id
  private String initiativeId;
  private String organizationId;
  private LocalDateTime lastUpdatedDateTime;

  private Long onboardedCitizenCount;
  private List<CommittedOffset> onboardingOutcomeCommittedOffsets;

  private Long accruedRewardsCents;
  private List<CommittedOffset> transactionEvaluationCommittedOffsets;

  @Data @AllArgsConstructor @FieldNameConstants
  public static class CommittedOffset{
    private int partition;
    private long offset;
  }

}
