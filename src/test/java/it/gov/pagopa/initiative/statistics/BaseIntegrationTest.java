package it.gov.pagopa.initiative.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.kafka.KafkaTestUtilitiesService;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.utils.TestIntegrationUtils;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.dto.events.OnboardingOutcomeDTO;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.dto.events.RewardNotificationDTO;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.test.fakers.OnboardingOutcomeDTOFaker;
import it.gov.pagopa.initiative.statistics.test.fakers.RewardNotificationDTOFaker;
import it.gov.pagopa.initiative.statistics.test.fakers.TransactionEvaluationDTOFaker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.util.Pair;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.PostConstruct;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@SpringBootTest
@EmbeddedKafka(topics = {
        "${app.kafka.producer.errors.topic}",
        "${app.kafka.consumer.onboarding-outcome.topic}",
        "${app.kafka.consumer.transaction-evaluation.topic}",
        "merchant-counters-transaction", // "${app.kafka.consumer.merchant-counters-transaction.topic}", Overridden in TestPropertySource
        "${app.kafka.consumer.merchant-counters-reward-notification.topic}",
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                // even if enabled into application.yml, spring test will not load it https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.jmx
                "spring.jmx.enabled=true",

                //region common feature disabled
                "app.reward-rule.cache.refresh-ms-rate=60000",
                "logging.level.it.gov.pagopa.common.kafka.service.ErrorNotifierServiceImpl=WARN",
                "logging.level.it.gov.pagopa.initiative.statistics=WARN",
                //endregion

                //region kafka brokers
                "logging.level.org.apache.zookeeper=WARN",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN",
                "logging.level.state.change.logger=WARN",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.kafka.binder.zkNodes=${spring.embedded.zookeeper.connect}",
                "app.kafka.consumer.onboarding-outcome.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "app.kafka.consumer.onboarding-outcome.security.protocol=PLAINTEXT",
                "app.kafka.consumer.transaction-evaluation.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "app.kafka.consumer.transaction-evaluation.security.protocol=PLAINTEXT",
                "app.kafka.consumer.merchant-counters-transaction.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "app.kafka.consumer.merchant-counters-transaction.security.protocol=PLAINTEXT",
                "app.kafka.consumer.merchant-counters-reward-notification.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "app.kafka.consumer.merchant-counters-reward-notification.security.protocol=PLAINTEXT",
                "app.kafka.producer.errors.bootstrap.servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.producer.security.protocol=PLAINTEXT",
                //endregion

                //region kafka test overrides
                "app.kafka.consumer.merchant-counters-transaction.topic=merchant-counters-transaction",
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.org.springframework.boot.autoconfigure.mongo.embedded=WARN",
                "spring.mongodb.embedded.version=4.0.21",
                //endregion
        })
@AutoConfigureDataMongo
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @Autowired
    protected KafkaTestUtilitiesService kafkaTestUtilitiesService;
    @Autowired
    protected MongoTestUtilitiesService mongoTestUtilitiesService;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${app.kafka.consumer.onboarding-outcome.topic}")
    protected String topicOnboardingOutcome;
    @Value("${app.kafka.consumer.transaction-evaluation.topic}")
    protected String topicTransactionEvaluation;
    @Value("${app.kafka.consumer.merchant-counters-transaction.topic}")
    protected String topicMerchantCountersTransaction;
    @Value("${app.kafka.consumer.merchant-counters-reward-notification.topic}")
    protected String topicMerchantCountersNotification;
    @Value("${app.kafka.producer.errors.topic}")
    protected String topicErrors;

    @Value("${app.kafka.consumer.onboarding-outcome.group-id}")
    protected String groupIdOnboardingOutcome;
    @Value("${app.kafka.consumer.transaction-evaluation.group-id}")
    protected String groupIdTransactionEvaluation;
    @Value("${app.kafka.consumer.merchant-counters-transaction.group-id}")
    protected String groupIdMerchantCountersTransaction;
    @Value("${app.kafka.consumer.merchant-counters-reward-notification.group-id}")
    protected String groupIdMerchantCountersNotification;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TestIntegrationUtils.setDefaultTimeZoneAndUnregisterCommonMBean();
    }

    @PostConstruct
    public void logEmbeddedServerConfig() throws NoSuchFieldException, UnknownHostException {
        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Embedded kafka: %s
                        ************************
                        """,
                mongoTestUtilitiesService.getMongoUrl(),
                kafkaTestUtilitiesService.getKafkaUrls()
                );
    }

    protected final Pattern errorUseCaseIdPatternMatch = getErrorUseCaseIdPatternMatch();
    protected void checkErrorsPublished(int expectedErrorMessagesNumber, long maxWaitingMs, List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
        kafkaTestUtilitiesService.checkErrorsPublished(topicErrors, errorUseCaseIdPatternMatch, expectedErrorMessagesNumber, maxWaitingMs, errorUseCases);
    }
    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, this::normalizePayload);
    }

    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, boolean expectRetryHeader, boolean expectedAppNameHeader) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, expectRetryHeader, expectedAppNameHeader, this::normalizePayload);
    }

    protected String normalizePayload(String payload){
        return payload;
    }

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"initiativeId\":\"id_([0-9]+)_?[^\"]*\"");
    }

    // region initiativeStatistics statistics utilities
    protected List<OnboardingOutcomeDTO> buildValidOnboardinOutcomesEntities(int bias, int size, String initiativeid) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> buildValidOnboardinOutcomeEntity(i, initiativeid))
                .toList();
    }
    protected OnboardingOutcomeDTO buildValidOnboardinOutcomeEntity(int bias, String initiativeid){
        return OnboardingOutcomeDTOFaker.mockInstance(bias, initiativeid);
    }

    protected List<TransactionEvaluationDTO> buildValidTransactionEvaluationEntities(int bias, int size, String initiativeid) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> buildValidTransactionEvaluationEntity(i, initiativeid))
                .toList();
    }
    protected TransactionEvaluationDTO buildValidTransactionEvaluationEntity(int bias, String initiativeid) {
        return TransactionEvaluationDTOFaker.mockInstanceBuilder(bias)
                .rewards(Map.of(initiativeid, new Reward(initiativeid, "ORGANIZATIONID_%s".formatted(initiativeid), BigDecimal.ONE, bias%3==0, bias%6==0)))
                .build();
    }
    protected List<RewardNotificationDTO> buildValidRewardNotificationEntities(int bias, int size, String initiativeid, boolean merchant) {
        return IntStream.range(bias, bias + size)
                .mapToObj(i -> buildValidRewardNotificationEntity(i, initiativeid, merchant))
                .toList();
    }
    protected RewardNotificationDTO buildValidRewardNotificationEntity(int bias, String initiativeid, boolean merchant) {
        return RewardNotificationDTOFaker.mockInstanceBuilder(bias, initiativeid, merchant)
                .rewardCents(100L)
                .build();
    }
    protected int getExpectedTrxsCount(int validMsgs) {
        int zeroBasedFix = validMsgs % 3 == 0 ? 0 : 1; // because 0 based, we have to add 1, but if using a multiple of 3, we have to remove one, compensating the 0 based bias
        return validMsgs
                - (validMsgs / 6 * 2 + zeroBasedFix) // each %6 will be a complete refund
                - (validMsgs / 3 - validMsgs / 6 + zeroBasedFix); // each %3 will be a refund
    }

    protected <T> long waitForCounterResult(String initiativeId, String organizationId, Function<T, Long> getterCounter, long expectedCounterValue, long maxWaitingMs, MongoRepository<T, String> statisticRepository) {
        int millisAttemptDelay = 500;
        int maxAttempts = (int) maxWaitingMs / millisAttemptDelay;

        long[] countSaved = {0};
        TestUtils.waitFor(() -> (countSaved[0] = statisticRepository.findById(buildCounterId(initiativeId))
                        .filter(r-> {
                            if (r instanceof InitiativeStatistics initiativeStatistics) {
                                Assertions.assertEquals(organizationId, initiativeStatistics.getOrganizationId());
                            }
                            return true;
                        })
                        .map(getterCounter).orElse(-1L)) >= expectedCounterValue
                , () -> "Expected %d counter value for initiative %s, read %d".formatted(expectedCounterValue, initiativeId, countSaved[0])
                , maxAttempts, millisAttemptDelay);
        return countSaved[0];
    }

    protected <T> long verifyPartitionOffsetStored(long expectOffsetSum, String initiativeId, Function<T, List<CommittedOffset>> getterStatisticsCommittedOffsets, boolean assertEquals, MongoRepository<T, String> statisticRepository) {
        T result = statisticRepository.findById(buildCounterId(initiativeId)).orElse(null);
        Assertions.assertNotNull(result);

        // -2 because offset start from 0 and we are using 2 partition for test
        long expectedOffsetSum0Based = expectOffsetSum - 2;
        long sum = getterStatisticsCommittedOffsets.apply(result).stream().mapToLong(CommittedOffset::getOffset).sum();

        if (assertEquals) {
            Assertions.assertEquals(expectedOffsetSum0Based, sum);
        } else {
            Assertions.assertTrue(expectedOffsetSum0Based>=sum, "Expected at least %d obtained %d".formatted(expectedOffsetSum0Based, sum));
        }

        return sum + 2;
    }

    protected abstract String buildCounterId(String initiativeId);
    //endregion
}
