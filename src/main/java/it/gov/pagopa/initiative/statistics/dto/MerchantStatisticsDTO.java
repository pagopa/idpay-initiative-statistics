package it.gov.pagopa.initiative.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MerchantStatisticsDTO {

    /**
     * The total amount
     */
    private BigDecimal amount;

    /**
     * The total accrued rewards of the merchant (amount - refunded)
     */
    private BigDecimal accrued;

    /**
     * The amount refunded to the merchant
     */
    private BigDecimal refunded;
}
