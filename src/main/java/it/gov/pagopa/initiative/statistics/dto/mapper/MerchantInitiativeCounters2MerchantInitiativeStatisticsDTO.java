package it.gov.pagopa.initiative.statistics.dto.mapper;

import it.gov.pagopa.initiative.statistics.dto.MerchantStatisticsDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;

import java.util.function.Function;

public class MerchantInitiativeCounters2MerchantInitiativeStatisticsDTO implements Function<MerchantInitiativeCounters, MerchantStatisticsDTO> {

    @Override
    public MerchantStatisticsDTO apply(MerchantInitiativeCounters merchantInitiativeCounters) {
        return MerchantStatisticsDTO.builder()
                .amount(merchantInitiativeCounters.getTotalAmount())
                .accrued(merchantInitiativeCounters.getTotalAmount().subtract(merchantInitiativeCounters.getTotalRefunded()))
                .refunded(merchantInitiativeCounters.getTotalRefunded())
                .build();
    }
}
