package it.gov.pagopa.initiative.statistics.model;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@Document("merchant_initiative_counters")
@FieldNameConstants
public class MerchantInitiativeCounters {
    @Id
    private String id;
    @NonNull
    private String merchantId;
    @NonNull
    private String initiativeId;

    /**
     * the total amount dispensed in cents
     */
    @Builder.Default
    private Long totalAmount = 0L;

    /**
     * the total amount already refunded in cents
     */
    @Builder.Default
    private Long totalRefunded = 0L;
    private long trxNumber;
    private long refundedNumber;

    private List<CommittedOffset> trxCommittedOffsets;
    private List<CommittedOffset> rewardNotificationCommittedOffsets;

    public MerchantInitiativeCounters(@NonNull String merchantId, @NonNull String initiativeId) {
        this.id = buildId(merchantId, initiativeId);
        this.merchantId = merchantId;
        this.initiativeId = initiativeId;
    }

    public static String buildId(@NonNull String merchantId, @NonNull String initiativeId) {
        return "%s_%s".formatted(merchantId, initiativeId);
    }

    public static String[] splitId(String id) {
        return id.split("_");
    }
}
