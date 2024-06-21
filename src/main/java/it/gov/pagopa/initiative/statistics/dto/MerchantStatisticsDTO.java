package it.gov.pagopa.initiative.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MerchantStatisticsDTO {

    /**
     * The total amount
     */
    private Long amountCents;

    /**
     * The total accrued rewards of the merchant (amount - refunded)
     */
    private Long accruedCents;

    /**
     * The amount refunded to the merchant
     */
    private Long refundedCents;
}
