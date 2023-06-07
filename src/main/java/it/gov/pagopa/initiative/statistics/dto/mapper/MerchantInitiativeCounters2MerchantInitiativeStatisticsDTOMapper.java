package it.gov.pagopa.initiative.statistics.dto.mapper;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.initiative.statistics.dto.MerchantStatisticsDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class MerchantInitiativeCounters2MerchantInitiativeStatisticsDTOMapper implements Function<MerchantInitiativeCounters, MerchantStatisticsDTO> {

    @Override
    public MerchantStatisticsDTO apply(MerchantInitiativeCounters merchantInitiativeCounters) {
        return MerchantStatisticsDTO.builder()
                .amount(CommonUtilities.centsToEuro(merchantInitiativeCounters.getTotalProvidedCents()))
                .accrued(CommonUtilities.centsToEuro(merchantInitiativeCounters.getTotalProvidedCents() - merchantInitiativeCounters.getTotalRefundedCents()))
                .refunded(CommonUtilities.centsToEuro(merchantInitiativeCounters.getTotalRefundedCents()))
                .build();
    }
}
