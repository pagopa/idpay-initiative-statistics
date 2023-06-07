package it.gov.pagopa.initiative.statistics.model;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@EqualsAndHashCode
@SuperBuilder(toBuilder = true, builderMethodName = "hiddenBuilder", buildMethodName = "hiddenBuild")
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
     * the total amount provided in cents
     */
    @Builder.Default
    private Long totalProvidedCents = 0L;

    /**
     * the total amount already refunded in cents
     */
    @Builder.Default
    private Long totalRefundedCents = 0L;
    private long trxNumber;
    private long refundedNumber;

    private List<CommittedOffset> trxCommittedOffsets;
    private List<CommittedOffset> rewardNotificationCommittedOffsets;

    private LocalDateTime lastUpdatedDateTime;

    public MerchantInitiativeCounters(@NonNull String merchantId, @NonNull String initiativeId) {
        this.id = buildId(merchantId, initiativeId);
        this.merchantId = merchantId;
        this.initiativeId = initiativeId;
    }

    public static String buildId(@NonNull String merchantId, @NonNull String initiativeId) {
        return "%s_%s".formatted(merchantId, initiativeId);
    }

    @SuppressWarnings("squid:S1452")
    public static MerchantInitiativeCountersBuilder<?,?> builder(String merchantId, String initiativeId){
        return MerchantInitiativeCounters.hiddenBuilder()
                .id(buildId(merchantId, initiativeId))
                .merchantId(merchantId)
                .initiativeId(initiativeId)
                .lastUpdatedDateTime(LocalDateTime.now());
    }


    @SuppressWarnings("squid:S1610") // suppressing conversion of abstract class into interface: this class is handled by lombok SuperBuilder
    public abstract static class MerchantInitiativeCountersBuilder<C extends MerchantInitiativeCounters, B extends MerchantInitiativeCountersBuilder<C, B>>  {

        public B merchantId(String merchantId){
            this.merchantId=merchantId;
            this.id=buildId(this.merchantId, this.initiativeId);
            return self();
        }

        public B initiativeId(String initiativeId){
            this.initiativeId=initiativeId;
            this.id=buildId(this.merchantId, this.initiativeId);
            return self();
        }

        public C build() {
            C out = this.hiddenBuild();
            out.setId(buildId(out.getMerchantId(), out.getInitiativeId()));
            return out;
        }
    }
}
