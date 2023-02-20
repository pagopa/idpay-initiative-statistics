package it.gov.pagopa.initiative.statistics.service.trx;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.service.ErrorNotifierService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

@Service
public class TransactionEvaluationStatisticsServiceImpl extends BaseStatisticsEvaluationService<TransactionEvaluationDTO, Reward> implements TransactionEvaluationStatisticsService {

    private final ErrorNotifierService errorNotifierService;
    private final InitiativeStatRepository initiativeStatRepository;

    public TransactionEvaluationStatisticsServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            ObjectMapper objectMapper,
            ErrorNotifierService errorNotifierService, InitiativeStatRepository initiativeStatRepository) {
        super(applicationName, objectMapper);

        this.errorNotifierService = errorNotifierService;
        this.initiativeStatRepository = initiativeStatRepository;
    }

    @Override
    protected Class<TransactionEvaluationDTO> getRecordClass() {
        return TransactionEvaluationDTO.class;
    }

    @Override
    protected String getFlowName() {
        return "TRANSACTION_EVALUATION";
    }

    @Override
    protected long retrieveLastProcessedOffset(String initiativeId, int partition, Reward reward) {
        return initiativeStatRepository.retrieveTransactionEvaluationCommittedOffset(initiativeId, reward.getOrganizationId(), partition);
    }

    @Override
    protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
        errorNotifierService.notifyTransactionEvaluation(message, description, false, exception);
    }

    @Override
    protected Stream<Reward> toInitiativeBasedEntityStream(TransactionEvaluationDTO transactionEvaluationDTO) {
        return transactionEvaluationDTO.getRewards().values().stream();
    }

    @Override
    protected String getInitiativeId(Reward reward) {
        return reward.getInitiativeId();
    }

    @Override
    protected void evaluateInitiative(String initiativeId, List<Reward> records, int partition, long maxOffset) {
        initiativeStatRepository.updateAccruedRewards(
                initiativeId,
                records.stream().map(Reward::getAccruedReward).reduce(BigDecimal::add).orElse(BigDecimal.ZERO),
                records.stream()
                        .filter(r -> r.getAccruedReward().compareTo(BigDecimal.ZERO) != 0)
                        .mapToLong(r -> {
                            if (r.isCompleteRefund()) {
                                return -1L;
                            } else if (r.isRefund()) {
                                return 0L;
                            } else {
                                return 1L;
                            }
                        }).sum(),
                partition, maxOffset);
    }
}
