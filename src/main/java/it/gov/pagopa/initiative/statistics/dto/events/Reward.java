package it.gov.pagopa.initiative.statistics.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reward {
    private String initiativeId;
    private String organizationId;

    /** The effective reward after CAP and REFUND evaluation */
    private BigDecimal accruedReward;

    /** True if it's a refunding reward */
    private boolean refund;
    /** True if it's a complete refunding reward */
    private boolean completeRefund;

    public Reward(String initiativeId, String organizationId, BigDecimal accruedReward){
        this(initiativeId, organizationId, accruedReward, false, false);
    }
}
