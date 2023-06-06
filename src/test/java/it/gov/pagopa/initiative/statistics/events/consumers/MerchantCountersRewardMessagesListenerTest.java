package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.initiative.statistics.dto.events.RewardNotificationDTO;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.service.merchant.counters.notification.MerchantNotificationStatisticsService;
import it.gov.pagopa.initiative.statistics.test.fakers.RewardNotificationDTOFaker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

class MerchantCountersRewardMessagesListenerTest extends BaseMerchantStatisticsMessageListenerTest {

    @SpyBean
    private MerchantNotificationStatisticsService merchantNotificationStatisticsService;

    @Test
    @Override
    void test(){
        super.test();
    }

    @Override
    protected StatisticsEvaluationService getStatisticsEvaluationServiceSpy() {
        return merchantNotificationStatisticsService;
    }

    @Override
    protected String getStatisticsMessagesTopic() {
        return topicMerchantCountersNotification;
    }

    @Override
    protected String getStatisticsMessagesGroupId() {
        return groupIdMerchantCountersNotification;
    }

    @Override
    protected List<RewardNotificationDTO> buildValidEntities(int bias, int size, String initiativeId) {
        return buildValidRewardNotificationEntities(bias, size, initiativeId, true);
    }

    @Override
    protected List<RewardNotificationDTO> buildSkippedEntities(int bias, int size) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> RewardNotificationDTOFaker.mockInstance(i, INITIATIVEID1, true))
                .toList();
    }

    @Override
    protected List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> getErrorUseCases() {
        return errorUseCases;
    }

    @Override
    protected long getExpectedCounterValue(int validMsgs) {
        return validMsgs;
    }

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp getErrorUseCaseIdPatternMatch
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"userId\":\"USERID([0-9]+)\"");
    }

    @Override
    protected String buildCounterId(String initiativeId) {
        return null;
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();

    {
        String jsonNotExpected = "{\"userId\":\"USERID0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> jsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[INITIATIVE_STATISTICS_EVALUATION][MERCHANT_COUNTERS_UPDATE_FROM_REWARD_NOTIFICATION] Unexpected json: {\"userId\":\"USERID0\",unexpectedStructure:0}", jsonNotExpected, null)
        ));

        String jsonNotValid = "{\"userId\":\"USERID1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[INITIATIVE_STATISTICS_EVALUATION][MERCHANT_COUNTERS_UPDATE_FROM_REWARD_NOTIFICATION] Unexpected json: {\"userId\":\"USERID1\",invalidJson", jsonNotValid, null)
        ));
    }
    //endregion
}
