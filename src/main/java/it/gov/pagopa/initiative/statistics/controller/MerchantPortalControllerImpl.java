package it.gov.pagopa.initiative.statistics.controller;

import it.gov.pagopa.initiative.statistics.dto.MerchantStatisticsDTO;
import it.gov.pagopa.initiative.statistics.service.merchant.counters.MerchantCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MerchantPortalControllerImpl implements MerchantPortalController {

    private final MerchantCountersService merchantCountersService;

    public MerchantPortalControllerImpl(MerchantCountersService merchantCountersService) {
        this.merchantCountersService = merchantCountersService;
    }

    @Override
    public ResponseEntity<MerchantStatisticsDTO> getMerchantInitiativeStatistics(String merchantId, String initiativeId) {
        return ResponseEntity.ok(
                merchantCountersService.getMerchantInitiativeStatistics(merchantId, initiativeId)
        );
    }
}
