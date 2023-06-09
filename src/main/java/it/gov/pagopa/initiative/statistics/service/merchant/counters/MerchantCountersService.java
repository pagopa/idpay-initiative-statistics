package it.gov.pagopa.initiative.statistics.service.merchant.counters;

import it.gov.pagopa.initiative.statistics.dto.MerchantStatisticsDTO;

public interface MerchantCountersService {

    MerchantStatisticsDTO getMerchantInitiativeStatistics(String merchantId, String initiativeId);
}
