package it.gov.pagopa.initiative.statistics.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RewardNotificationDTO {
  private String id;
  private String externalId;
  private String rewardNotificationId;
  private String initiativeId;
  private String beneficiaryId;
  private String organizationId;
  private String iban;
  private String status;
  private String rewardStatus;
  private String refundType;
  private Long rewardCents;
  private Long effectiveRewardCents;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDateTime feedbackDate;
  private String rejectionCode;
  private String rejectionReason;
  private Long feedbackProgressive;
  private LocalDate executionDate;
  private LocalDate transferDate;
  private LocalDate userNotificationDate;
  private String cro;
}
