package it.gov.pagopa.initiative.statistics.config;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.initiative.statistics.constants.InitiativeConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatisticsErrorManagerConfig {
    @Bean
    ErrorDTO defaultErrorDTO() {
        return new ErrorDTO(
                InitiativeConstants.ExceptionCode.GENERIC_ERROR,
                "A generic error occurred"
        );
    }

    @Bean
    ErrorDTO tooManyRequestsErrorDTO() {
        return new ErrorDTO(InitiativeConstants.ExceptionCode.TOO_MANY_REQUESTS, "Too Many Requests");
    }

    @Bean
    ErrorDTO templateValidationErrorDTO(){
        return new ErrorDTO(InitiativeConstants.ExceptionCode.INVALID_REQUEST, null);
    }
}
