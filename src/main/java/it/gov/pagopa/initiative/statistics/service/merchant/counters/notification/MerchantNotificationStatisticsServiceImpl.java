package it.gov.pagopa.initiative.statistics.service.merchant.counters.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.initiative.statistics.dto.events.RewardNotificationDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.service.BaseStatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.service.StatisticsErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class MerchantNotificationStatisticsServiceImpl extends BaseStatisticsEvaluationService<RewardNotificationDTO, RewardNotificationDTO> implements MerchantNotificationStatisticsService {

    private final StatisticsErrorNotifierService statisticsErrorNotifierService;
    private final MerchantInitiativeCountersRepository merchantCountersRepository;

    protected MerchantNotificationStatisticsServiceImpl(@Value("${spring.application.name}") String applicationName,
                                                        @Value("${app.kafka.consumer.merchant-counters-reward-notification.group-id}") String consumerGroup,
                                                        ObjectMapper objectMapper,
                                                        StatisticsErrorNotifierService statisticsErrorNotifierService,
                                                        MerchantInitiativeCountersRepository merchantCountersRepository) {
        super(applicationName, consumerGroup, objectMapper);

        this.statisticsErrorNotifierService = statisticsErrorNotifierService;
        this.merchantCountersRepository = merchantCountersRepository;
    }


    @Override
    protected Class<RewardNotificationDTO> getRecordClass() {
        return RewardNotificationDTO.class;
    }

    @Override
    protected String getFlowName() {
        return "MERCHANT_COUNTERS_UPDATE_FROM_REWARD_NOTIFICATION";
    }

    @Override
    protected long retrieveLastProcessedOffset(String counterId, int partition, RewardNotificationDTO right) {
        return merchantCountersRepository.retrieveMerchantCountersNotificationCommittedOffset(counterId, partition);
    }

    @Override
    protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
        statisticsErrorNotifierService.notifyMerchantCountersRewardNotification(message, description, false, exception);
    }

    @Override
    protected Stream<RewardNotificationDTO> toInitiativeBasedEntityStream(RewardNotificationDTO rewardNotificationDTO) {
        return Stream.of(rewardNotificationDTO);
    }

    @Override
    protected void evaluateCounter(String counterId, List<RewardNotificationDTO> records, int partition, long maxOffset) {
        merchantCountersRepository.updateCountersFromRewardNotification(
                counterId,
                aggregateReward(records),
                aggregateTrxNumber(records),
                partition,
                maxOffset);
    }

    @Override
    protected String getCounterId(RewardNotificationDTO rewardNotification) {
        return MerchantInitiativeCounters.buildId(rewardNotification.getBeneficiaryId(), rewardNotification.getInitiativeId());
    }

    private Long aggregateReward(List<RewardNotificationDTO> records) {
        return records.stream().map(RewardNotificationDTO::getRewardCents).reduce(Long::sum).orElse(0L);
    }

    private long aggregateTrxNumber(List<RewardNotificationDTO> records) {
        return records.stream()
                .filter(r -> r.getRewardCents() != 0)
                // TODO handle refundType?
                .mapToLong(r -> 1L)
                .sum();
    }
}
