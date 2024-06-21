package it.gov.pagopa.initiative.statistics.service.trx;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.service.StatisticsErrorNotifierService;
import it.gov.pagopa.initiative.statistics.utils.Constants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class TransactionEvaluationStatisticsServiceImpl extends BaseStatisticsEvaluationService<TransactionEvaluationDTO, Reward> implements TransactionEvaluationStatisticsService {

    private final StatisticsErrorNotifierService statisticsErrorNotifierService;
    private final InitiativeStatRepository initiativeStatRepository;

    public TransactionEvaluationStatisticsServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${app.kafka.consumer.transaction-evaluation.group-id}") String consumerGroup,
            ObjectMapper objectMapper,
            StatisticsErrorNotifierService statisticsErrorNotifierService,
            InitiativeStatRepository initiativeStatRepository) {
        super(applicationName, consumerGroup, objectMapper);

        this.statisticsErrorNotifierService = statisticsErrorNotifierService;
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
    protected long retrieveLastProcessedOffset(String counterId, int partition, Reward reward) {
        return initiativeStatRepository.retrieveTransactionEvaluationCommittedOffset(counterId, reward.getOrganizationId(), partition);
    }

    @Override
    protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
        statisticsErrorNotifierService.notifyTransactionEvaluation(message, description, false, exception);
    }

    @Override
    protected Stream<Reward> toInitiativeBasedEntityStream(TransactionEvaluationDTO transactionEvaluationDTO) {
        return trxEvaluationDto2InitiativeBasedEntityStream(transactionEvaluationDTO);
    }

    public static Stream<Reward> trxEvaluationDto2InitiativeBasedEntityStream(TransactionEvaluationDTO transactionEvaluationDTO) {
        return transactionEvaluationDTO.getRewards() != null && Constants.TRX_STATUS_REWARDED.equals(transactionEvaluationDTO.getStatus())
                ? transactionEvaluationDTO.getRewards().values().stream()
                : Stream.empty();
    }

    @Override
    protected String getCounterId(Reward reward) {
        return reward.getInitiativeId();
    }

    @Override
    protected void evaluateCounter(String counterId, List<Reward> records, int partition, long maxOffset) {
        initiativeStatRepository.updateAccruedRewards(
                counterId,
                aggregateReward(records),
                aggregateTrxNumber(records),
                partition,
                maxOffset);
    }

    public static long aggregateTrxNumber(List<Reward> records) {
        return records.stream()
                .filter(r -> r.getAccruedRewardCents().compareTo(0L) != 0)
                .mapToLong(r -> {
                    if (r.isCompleteRefund()) {
                        return -1L;
                    } else if (r.isRefund()) {
                        return 0L;
                    } else {
                        return 1L;
                    }
                }).sum();
    }

    public static Long aggregateReward(List<Reward> records) {
        return records.stream().map(Reward::getAccruedRewardCents).reduce(Long::sum).orElse(0L);
    }
}
