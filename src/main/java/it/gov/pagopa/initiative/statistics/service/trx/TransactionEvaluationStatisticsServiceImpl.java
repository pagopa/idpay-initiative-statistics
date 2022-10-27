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
import java.util.Map;
import java.util.stream.Stream;

@Service
public class TransactionEvaluationStatisticsServiceImpl extends BaseStatisticsEvaluationService<TransactionEvaluationDTO, Map.Entry<String, Reward>> implements TransactionEvaluationStatisticsService {

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
    protected long retrieveLastProcessedOffset(String initiativeId, int partition, Map.Entry<String, Reward> reward) {
        return initiativeStatRepository.retrieveTransactionEvaluationCommittedOffset(initiativeId, partition);
    }

    @Override
    protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
        errorNotifierService.notifyTransactionEvaluation(message, description, false, exception);
    }

    @Override
    protected Stream<Map.Entry<String, Reward>> toInitiativeBasedEntityStream(TransactionEvaluationDTO transactionEvaluationDTO) {
        return transactionEvaluationDTO.getRewards().entrySet().stream();
    }

    @Override
    protected String getInitiativeId(Map.Entry<String, Reward> t) {
        return t.getKey();
    }

    @Override
    protected void evaluateInitiative(String initiativeId, List<Map.Entry<String, Reward>> records, int partition, long maxOffset) {
        initiativeStatRepository.updateAccruedRewards(
                initiativeId,
                records.stream().map(r -> r.getValue().getAccruedReward()).reduce(BigDecimal::add).orElse(BigDecimal.ZERO),
                partition, maxOffset);
    }
}
