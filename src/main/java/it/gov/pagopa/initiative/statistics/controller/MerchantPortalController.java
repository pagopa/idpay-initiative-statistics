package it.gov.pagopa.initiative.statistics.controller;

import it.gov.pagopa.initiative.statistics.dto.MerchantStatisticsDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/idpay/merchant/portal")
public interface MerchantPortalController {

    @GetMapping(value = "/initiatives/{initiativeId}/statistics",
            produces = {"application/json"})
    MerchantStatisticsDTO getMerchantInitiativeStatistics(
            @RequestHeader("x-merchant-id") String merchantId,
            @PathVariable("initiativeId")  String initiativeId);

}
