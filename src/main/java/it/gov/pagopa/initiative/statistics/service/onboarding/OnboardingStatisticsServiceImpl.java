package it.gov.pagopa.initiative.statistics.service.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.initiative.statistics.dto.events.OnboardingOutcomeDTO;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.service.ErrorNotifierService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class OnboardingStatisticsServiceImpl extends BaseStatisticsEvaluationService<OnboardingOutcomeDTO, OnboardingOutcomeDTO> implements OnboardingStatisticsService {

    private final ErrorNotifierService errorNotifierService;
    private final InitiativeStatRepository initiativeStatRepository;

    public OnboardingStatisticsServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            ObjectMapper objectMapper,
            ErrorNotifierService errorNotifierService, InitiativeStatRepository initiativeStatRepository) {
        super(applicationName, objectMapper);

        this.errorNotifierService = errorNotifierService;
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
    protected long retrieveLastProcessedOffset(String initiativeId, int partition, OnboardingOutcomeDTO onboardinOutcome) {
        return initiativeStatRepository.retrieveOnboardingOutcomeCommittedOffset(initiativeId, onboardinOutcome.getOrganizationId(), partition);
    }

    @Override
    protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
        errorNotifierService.notifyOnboardingOutcome(message, description, false, exception);
    }

    @Override
    protected Stream<OnboardingOutcomeDTO> toInitiativeBasedEntityStream(OnboardingOutcomeDTO onboardingOutcomeDTO) {
        return Stream.of(onboardingOutcomeDTO);
    }

    @Override
    protected String getInitiativeId(OnboardingOutcomeDTO t) {
        return t.getInitiativeId();
    }

    @Override
    protected void evaluateInitiative(String initiativeId, List<OnboardingOutcomeDTO> records, int partition, long maxOffset) {
        initiativeStatRepository.updateOnboardingCount(
                initiativeId,
                records.stream().filter(o->"ONBOARDING_OK".equals(o.getStatus())).count(),
                partition, maxOffset
        );
    }
}
