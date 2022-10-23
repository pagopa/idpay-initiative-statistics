package it.gov.pagopa.initiative.statistics.controller;

import it.gov.pagopa.initiative.statistics.dto.InitiativeStatisticsDTO;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.service.InitiativeStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class InitiativeApiController implements InitiativeApi {

    @Autowired
    private InitiativeStatService initiativeStatService;

    public InitiativeApiController(InitiativeStatService initiativeStatService) {
        this.initiativeStatService = initiativeStatService;
    }

    @Override
    public ResponseEntity<InitiativeStatisticsDTO> initiativeStatistics(String organizationId, String initiativeId) {

        InitiativeStatistics stat = this.initiativeStatService.getStatistics(organizationId, initiativeId);
        return ResponseEntity.ok(InitiativeStatisticsDTO.builder()
                        .onboardedCitizenCount(stat.getOnboardedCitizenCount())
                        .accruedRewards(stat.getAccruedRewards().toString())
                        .lastUpdatedDateTime(stat.getLastUpdatedDateTime())
                .build());
    }
}
