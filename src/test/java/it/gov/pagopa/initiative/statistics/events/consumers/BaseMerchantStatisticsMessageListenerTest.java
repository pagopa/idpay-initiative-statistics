package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
        return MerchantInitiativeCounters.builder()
                .id(buildCounterId(initiativeId))
                .merchantId(MERCHANTID)
                .initiativeId(initiativeId)
                .build();
    }

    @Override
    protected String buildCounterId(String initiativeId) {
        return "%s_%s".formatted(MERCHANTID, initiativeId);
    }

    @Override
    protected Function<MerchantInitiativeCounters, Long> getGetterCounter() {
        return MerchantInitiativeCounters::getTotalAmount;
    }

    @Override
    protected BiConsumer<MerchantInitiativeCounters, Long> getSetterCounter() {
        return MerchantInitiativeCounters::setTotalAmount;
    }

    @Override
    protected Function<MerchantInitiativeCounters, List<CommittedOffset>> getGetterStatisticsCommittedOffsets() {
        return MerchantInitiativeCounters::getTrxCommittedOffsets;
    }

    @Override
    protected BiConsumer<MerchantInitiativeCounters, List<CommittedOffset>> getSetterStatisticsCommittedOffsets() {
        return MerchantInitiativeCounters::setTrxCommittedOffsets;
    }
}
