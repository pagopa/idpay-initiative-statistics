package it.gov.pagopa.initiative.statistics.service.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.initiative.statistics.dto.events.OnboardingOutcomeDTO;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.service.StatisticsErrorNotifierService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class OnboardingStatisticsServiceImpl extends BaseStatisticsEvaluationService<OnboardingOutcomeDTO, OnboardingOutcomeDTO> implements OnboardingStatisticsService {

    private final StatisticsErrorNotifierService statisticsErrorNotifierService;
    private final InitiativeStatRepository initiativeStatRepository;

    public OnboardingStatisticsServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${app.kafka.consumer.onboarding-outcome.group-id}") String consumerGroup,
            ObjectMapper objectMapper,
            StatisticsErrorNotifierService statisticsErrorNotifierService, InitiativeStatRepository initiativeStatRepository) {
        super(applicationName, consumerGroup, objectMapper);

        this.statisticsErrorNotifierService = statisticsErrorNotifierService;
        this.initiativeStatRepository = initiativeStatRepository;
    }

    @Override
    protected Class<OnboardingOutcomeDTO> getRecordClass() {
        return OnboardingOutcomeDTO.class;
    }

    @Override
    protected String getFlowName() {
        return "ONBOARDING_OUTCOME";
    }

    @Override
    protected long retrieveLastProcessedOffset(String counterId, int partition, OnboardingOutcomeDTO onboardinOutcome) {
        return initiativeStatRepository.retrieveOnboardingOutcomeCommittedOffset(counterId, onboardinOutcome.getOrganizationId(), partition);
    }

    @Override
    protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
        statisticsErrorNotifierService.notifyOnboardingOutcome(message, description, false, exception);
    }

    @Override
    protected Stream<OnboardingOutcomeDTO> toInitiativeBasedEntityStream(OnboardingOutcomeDTO onboardingOutcomeDTO) {
        return Stream.of(onboardingOutcomeDTO);
    }

    @Override
    protected String getCounterId(OnboardingOutcomeDTO t) {
        return t.getInitiativeId();
    }

    @Override
    protected void evaluateCounter(String counterId, List<OnboardingOutcomeDTO> records, int partition, long maxOffset) {
        initiativeStatRepository.updateOnboardingCount(
                counterId,
                records.stream().filter(o->"ONBOARDING_OK".equals(o.getStatus())).count(),
                partition, maxOffset
        );
    }
}
