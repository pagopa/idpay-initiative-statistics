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
                .amount(CommonUtilities.centsToEuro(merchantInitiativeCounters.getTotalAmount()))
                .accrued(CommonUtilities.centsToEuro(merchantInitiativeCounters.getTotalAmount() - merchantInitiativeCounters.getTotalRefunded()))
                .refunded(CommonUtilities.centsToEuro(merchantInitiativeCounters.getTotalRefunded()))
                .build();
    }
}
