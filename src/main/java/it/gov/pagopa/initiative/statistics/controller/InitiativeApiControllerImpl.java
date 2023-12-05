package it.gov.pagopa.initiative.statistics.controller;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.initiative.statistics.dto.InitiativeStatisticsDTO;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.service.InitiativeStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

@RestController
@Slf4j
public class InitiativeApiControllerImpl implements InitiativeApiController {

    public static final DecimalFormatSymbols decimalFormatterSymbols = new DecimalFormatSymbols();
    static{
        decimalFormatterSymbols.setDecimalSeparator(',');
    }

    private final InitiativeStatService initiativeStatService;

    @Autowired
    public InitiativeApiControllerImpl(InitiativeStatService initiativeStatService) {
        this.initiativeStatService = initiativeStatService;
    }

    @Override
    public ResponseEntity<InitiativeStatisticsDTO> initiativeStatistics(String organizationId, String initiativeId) {
        log.info("Requesting statistics for organization {} and initiative {}", organizationId, initiativeId);

        InitiativeStatistics stat = this.initiativeStatService.getStatistics(organizationId, initiativeId);
        return ResponseEntity.ok(InitiativeStatisticsDTO.builder()
                        .onboardedCitizenCount(stat.getOnboardedCitizenCount())
                        .rewardedTrxs(stat.getRewardedTrxs())
                        .accruedRewards(CommonUtilities.doubleToBigDecimal(stat.getAccruedRewardsCents()))
                        .lastUpdatedDateTime(stat.getLastUpdatedDateTime())
                .build());
    }
}
