package it.gov.pagopa.initiative.statistics.events.consumers;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.service.StatisticsEvaluationService;
import it.gov.pagopa.initiative.statistics.service.merchant.counters.trx.MerchantTransactionStatisticsService;
import it.gov.pagopa.initiative.statistics.test.fakers.TransactionEvaluationDTOFaker;
import it.gov.pagopa.initiative.statistics.utils.Constants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

class MerchantCountersTransactionMessagesListenerTest extends BaseMerchantStatisticsMessageListenerTest {

    @SpyBean
    private MerchantTransactionStatisticsService merchantTransactionStatisticsService;

    @Test
    @Override
    void test(){
        super.test();
    }

    @Override
    protected StatisticsEvaluationService getStatisticsEvaluationServiceSpy() {
        return merchantTransactionStatisticsService;
    }

    @Override
    protected String getStatisticsMessagesTopic() {
        return topicMerchantCountersTransaction;
    }

    @Override
    protected String getStatisticsMessagesGroupId() {
        return groupIdMerchantCountersTransaction;
    }

    @Override
    protected List<TransactionEvaluationDTO> buildValidEntities(int bias, int size, String initiativeId) {
        List<TransactionEvaluationDTO> out = buildValidTransactionEvaluationEntities(bias, size, initiativeId);
        out.forEach(t -> {
            t.setRewards(new HashMap<>(t.getRewards()));
            t.getRewards().put(initiativeId+"_2", new Reward(initiativeId+"_2", "ORGANIZATIONID_"+initiativeId, BigDecimal.valueOf(2)));
        });
        return out;
    }

    @Override
    protected List<TransactionEvaluationDTO> buildSkippedEntities(int bias, int size) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> {
                    TransactionEvaluationDTO out = TransactionEvaluationDTOFaker.mockInstance(i, INITIATIVEID1);
                    if(i%4==0) {
                        out.setRewards(null);
                    } else if(i%4==1) {
                        out.setRewards(Collections.emptyMap());
                    } else  if(i%4==2) {
                        out.setRewards(Map.of(INITIATIVEID1, new Reward(INITIATIVEID1, "ORGANIZATIONID", BigDecimal.ZERO)));
                    } else {
                        out.setStatus(Constants.TRX_STATUS_AUTHORIZED);
                    }
                    return out;
                })
                .toList();
    }

    @Override
    protected List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> getErrorUseCases() {
        return errorUseCases;
    }

    @Override
    protected long getExpectedCounterValue(int validMsgs) {
        return validMsgs * 100L;
    }

    @Override
    protected void publishIntoEmbeddedKafka(Integer partition, String key, String payload) {
        if(key==null){
            key = TestUtils.readJsonStringFieldValue(payload, "userId");
        }
        super.publishIntoEmbeddedKafka(partition, key, payload);
    }

    @Override
    protected long waitForCounterResult(String initiativeId, String organizationId, long expectedCounterValue, long maxWaitingMs) {
        super.waitForCounterResult(initiativeId+"_2", organizationId, expectedCounterValue*2, maxWaitingMs);
        return super.waitForCounterResult(initiativeId, organizationId, expectedCounterValue, maxWaitingMs);
    }

    @Override
    protected long verifyPartitionOffsetStored(long expectOffsetSum, String initiativeid, boolean assertEquals) {
        long i1Offsets = super.verifyPartitionOffsetStored(expectOffsetSum, initiativeid, assertEquals);
        long i2Offsets = super.verifyPartitionOffsetStored(expectOffsetSum, initiativeid + "_2", assertEquals);
        return Math.max(i1Offsets, i2Offsets);
    }

    @Override
    protected long checkResults(int validMsgs, long maxWaitingMs) {
        long out = super.checkResults(validMsgs, maxWaitingMs);

        int expectedTrxsCount = getExpectedTrxsCount(validMsgs);
        Assertions.assertEquals(expectedTrxsCount, getStatRepository().findById(buildCounterId(INITIATIVEID1)).map(MerchantInitiativeCounters::getTrxNumber).orElse(null));
        Assertions.assertEquals(expectedTrxsCount, getStatRepository().findById(buildCounterId(INITIATIVEID2)).map(MerchantInitiativeCounters::getTrxNumber).orElse(null));

        return out;
    }

    @Override
    protected String buildCounterId(String initiativeId) {
        return "%s_%s".formatted(MERCHANTID, initiativeId);
    }



    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp getErrorUseCaseIdPatternMatch
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"userId\":\"USERID([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();

    {
        String jsonNotExpected = "{\"userId\":\"USERID0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> jsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[INITIATIVE_STATISTICS_EVALUATION][MERCHANT_COUNTERS_UPDATE_FROM_TRANSACTION] Unexpected json: {\"userId\":\"USERID0\",unexpectedStructure:0}", jsonNotExpected, "USERID0")
        ));

        String jsonNotValid = "{\"userId\":\"USERID1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[INITIATIVE_STATISTICS_EVALUATION][MERCHANT_COUNTERS_UPDATE_FROM_TRANSACTION] Unexpected json: {\"userId\":\"USERID1\",invalidJson", jsonNotValid, "USERID1")
        ));
    }
    //endregion
}
