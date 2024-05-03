package it.gov.pagopa.initiative.statistics.controller;

import it.gov.pagopa.initiative.statistics.dto.InitiativeStatisticsDTO;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.service.InitiativeStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;

@RestController
@Slf4j
public class InitiativeApiControllerImpl implements InitiativeApiController {

    public static final DecimalFormatSymbols decimalFormatterSymbols = new DecimalFormatSymbols();

    static{
        decimalFormatterSymbols.setDecimalSeparator(',');
    }



    private final InitiativeStatService initiativeStatService;

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
                        .accruedRewards(centsToEuro(stat.getAccruedRewardsCents()))
                        .lastUpdatedDateTime(stat.getLastUpdatedDateTime())
                .build());
    }

    private static BigDecimal centsToEuro(Long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);
    }
}
