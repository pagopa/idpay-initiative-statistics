package it.gov.pagopa.initiative.statistics.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantReward {
    private String merchantId;
    private Reward reward;
}
