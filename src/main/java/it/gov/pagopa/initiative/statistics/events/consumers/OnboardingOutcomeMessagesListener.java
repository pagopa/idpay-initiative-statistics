package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.service.onboarding.OnboardingStatisticsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnboardingOutcomeMessagesListener extends BaseStatisticsEvaluatorMessagesListener {
    public OnboardingOutcomeMessagesListener(OnboardingStatisticsService onboardingStatisticsService) {
        super(onboardingStatisticsService);
    }
}
