package it.gov.pagopa.initiative.statistics.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
  private String initiativeStatisticsId;
  private String initiativeId;
  private String organizationId;
  private OffsetDateTime lastUpdatedDateTime;
  private Integer onboardedCitizenCount;
  private BigDecimal accruedRewards;

}
