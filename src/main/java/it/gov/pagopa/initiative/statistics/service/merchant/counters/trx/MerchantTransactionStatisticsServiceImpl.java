package it.gov.pagopa.initiative.statistics.service.merchant.counters.trx;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.initiative.statistics.dto.events.MerchantReward;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationServiceUtilities;
import it.gov.pagopa.initiative.statistics.service.StatisticsErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.trx.TransactionEvaluationStatisticsServiceImpl;
import it.gov.pagopa.initiative.statistics.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class MerchantTransactionStatisticsServiceImpl extends StatisticsEvaluationServiceUtilities<TransactionEvaluationDTO, MerchantReward> implements MerchantTransactionStatisticsService {

    private final StatisticsErrorNotifierService statisticsErrorNotifierService;
    private final MerchantInitiativeCountersRepository merchantCountersRepository;

    protected MerchantTransactionStatisticsServiceImpl(@Value("${spring.application.name}") String applicationName,
                                                       @Value("${app.kafka.consumer.merchant-counters-transaction.group-id}") String consumerGroup,
                                                       ObjectMapper objectMapper,
                                                       StatisticsErrorNotifierService statisticsErrorNotifierService,
                                                       MerchantInitiativeCountersRepository merchantCountersRepository) {
        super(applicationName, consumerGroup, objectMapper);

        this.statisticsErrorNotifierService = statisticsErrorNotifierService;
        this.merchantCountersRepository = merchantCountersRepository;
    }


    @Override
    protected Class<TransactionEvaluationDTO> getRecordClass() {
        return TransactionEvaluationDTO.class;
    }

    @Override
    protected String getFlowName() {
        return "MERCHANT_COUNTERS_UPDATE_FROM_TRANSACTION";
    }

    @Override
    protected long retrieveLastProcessedOffset(String counterId, int partition, MerchantReward merchantReward) {
        return merchantCountersRepository.retrieveMerchantCountersTransactionCommittedOffset(counterId, merchantReward.getMerchantId(), merchantReward.getReward().getInitiativeId(), partition);
    }

    @Override
    protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
        statisticsErrorNotifierService.notifyMerchantCountersTransaction(message, description, false, exception);
    }

    @Override
    protected Stream<MerchantReward> toInitiativeBasedEntityStream(TransactionEvaluationDTO transactionEvaluationDTO) {
        return trxEvaluationDto2InitiativeBasedEntityStream(transactionEvaluationDTO)
                .map(r -> new MerchantReward(transactionEvaluationDTO.getMerchantId(), r));
    }

    public static Stream<Reward> trxEvaluationDto2InitiativeBasedEntityStream(TransactionEvaluationDTO transactionEvaluationDTO) {
        return transactionEvaluationDTO.getRewards() != null
                && !Constants.EXCLUDED_TRX_STATUSES.contains(transactionEvaluationDTO.getStatus())
                && transactionEvaluationDTO.getMerchantId() != null
                ? transactionEvaluationDTO.getRewards().values().stream()
                : Stream.empty();
    }

    @Override
    protected String getCounterId(MerchantReward merchantReward) {
        return MerchantInitiativeCounters.buildId(merchantReward.getMerchantId(), merchantReward.getReward().getInitiativeId());
    }

    @Override
    protected void evaluateCounter(String counterId, List<MerchantReward> records, int partition, long maxOffset) {
        List<Reward> rewards = records.stream().map(MerchantReward::getReward).toList();
        merchantCountersRepository.updateCountersFromTransaction(
                counterId,
                TransactionEvaluationStatisticsServiceImpl.aggregateReward(rewards),
                TransactionEvaluationStatisticsServiceImpl.aggregateTrxNumber(rewards),
                partition,
                maxOffset);
    }
}
