package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.dto.events.RewardNotificationDTO;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseMerchantStatisticsMessageListenerTest extends BaseStatisticsMessagesListenerTest<MerchantInitiativeCounters> {
    protected static final String MERCHANTID = "MERCHANTID";

    @Autowired
    protected MerchantInitiativeCountersRepository merchantInitiativeCountersRepository;

    @Override
    protected MerchantInitiativeCountersRepository getStatRepository() {
        return merchantInitiativeCountersRepository;
    }

    @Override
    protected MerchantInitiativeCounters buildStatisticInstance(String initiativeId) {
        return MerchantInitiativeCounters.builder(MERCHANTID, initiativeId).build();
    }

    @Override
    protected String buildCounterId(String initiativeId) {
        return MerchantInitiativeCounters.buildId(MERCHANTID, initiativeId);
    }

    @Override
    protected TransactionEvaluationDTO buildValidTransactionEvaluationEntity(int bias, String initiativeid) {
        TransactionEvaluationDTO out = super.buildValidTransactionEvaluationEntity(bias, initiativeid);
        out.setMerchantId(MERCHANTID);
        return out;
    }

    @Override
    protected RewardNotificationDTO buildValidRewardNotificationEntity(int bias, String initiativeid, boolean merchant) {
        RewardNotificationDTO out = super.buildValidRewardNotificationEntity(bias, initiativeid, merchant);
        out.setBeneficiaryId(MERCHANTID);
        return out;
    }

    @Override
    protected void checkAndEmptyTimestampFields(MerchantInitiativeCounters retrieved) {
        Assertions.assertNotNull(retrieved.getLastUpdatedDateTime());

        retrieved.setLastUpdatedDateTime(null);
    }
}
