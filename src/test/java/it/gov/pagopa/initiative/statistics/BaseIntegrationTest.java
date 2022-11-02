package it.gov.pagopa.initiative.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.runtime.Executable;
import it.gov.pagopa.initiative.statistics.dto.events.OnboardingOutcomeDTO;
import it.gov.pagopa.initiative.statistics.dto.events.Reward;
import it.gov.pagopa.initiative.statistics.dto.events.TransactionEvaluationDTO;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.initiative.statistics.test.fakers.OnboardingOutcomeDTOFaker;
import it.gov.pagopa.initiative.statistics.test.fakers.TransactionEvaluationDTOFaker;
import it.gov.pagopa.initiative.statistics.test.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.utils.Constants;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(topics = {
        "${app.kafka.producer.errors.topic}",
        "${app.kafka.consumer.onboarding-outcome.topic}",
        "${app.kafka.consumer.transaction-evaluation.topic}",
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                // even if enabled into application.yml, spring test will not load it https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.jmx
                "spring.jmx.enabled=true",

                //region common feature disabled
                "app.reward-rule.cache.refresh-ms-rate=60000",
                "logging.level.it.gov.pagopa.initiative.statistics.service.ErrorNotifierServiceImpl=WARN",
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
                "app.kafka.producer.errors.bootstrap.servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.producer.security.protocol=PLAINTEXT",
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.org.springframework.boot.autoconfigure.mongo.embedded=WARN",
                "spring.mongodb.embedded.version=4.0.21",
                //endregion
        })
@AutoConfigureDataMongo
public abstract class BaseIntegrationTest {
    public static final String APPLICATION_NAME = "idpay-initiative-statistics";

    @Autowired
    protected EmbeddedKafkaBroker kafkaBroker;
    @Autowired
    protected KafkaTemplate<String, String> template;

    @Autowired(required = false)
    private MongodExecutable embeddedMongoServer;

    @Value("${spring.data.mongodb.uri}")
    private String mongodbUri;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.cloud.stream.kafka.binder.zkNodes}")
    private String zkNodes;

    @Value("${app.kafka.consumer.onboarding-outcome.topic}")
    protected String topicOnboardingOutcome;
    @Value("${app.kafka.consumer.transaction-evaluation.topic}")
    protected String topicTransactionEvaluation;
    @Value("${app.kafka.producer.errors.topic}")
    protected String topicErrors;

    @Value("${app.kafka.consumer.onboarding-outcome.group-id}")
    protected String groupIdOnboardingOutcome;
    @Value("${app.kafka.consumer.transaction-evaluation.group-id}")
    protected String groupIdTransactionEvaluation;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TimeZone.setDefault(TimeZone.getTimeZone(Constants.ZONEID));

        unregisterMBean("kafka.*:*");
        unregisterMBean("org.springframework.*:*");
    }

    private static void unregisterMBean(String objectName) throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        ObjectName mbeanName = new ObjectName(objectName);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        for (ObjectInstance mBean : mBeanServer.queryMBeans(mbeanName, null)) {
            mBeanServer.unregisterMBean(mBean.getObjectName());
        }
    }

    @PostConstruct
    public void logEmbeddedServerConfig() throws NoSuchFieldException, UnknownHostException {
        String mongoUrl;
        if(embeddedMongoServer != null) {
            Field mongoEmbeddedServerConfigField = Executable.class.getDeclaredField("config");
            mongoEmbeddedServerConfigField.setAccessible(true);
            MongodConfig mongodConfig = (MongodConfig) ReflectionUtils.getField(mongoEmbeddedServerConfigField, embeddedMongoServer);
            Net mongodNet = Objects.requireNonNull(mongodConfig).net();

            mongoUrl="mongodb://%s:%s".formatted(mongodNet.getServerAddress().getHostAddress(), mongodNet.getPort());
        } else {
            mongoUrl=mongodbUri.replaceFirst(":[^:]+(?=:[0-9]+)", "");
        }

        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Embedded kafka: %s
                        ************************
                        """,
                mongoUrl,
                "bootstrapServers: %s, zkNodes: %s".formatted(bootstrapServers, zkNodes)
                );
    }

    protected Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId) {
        return getEmbeddedKafkaConsumer(topic, groupId, true);
    }

    protected Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId, boolean attachToBroker) {
        if (!kafkaBroker.getTopics().contains(topic)) {
            kafkaBroker.addTopics(topic);
        }

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", kafkaBroker);
        consumerProps.put("key.deserializer", StringDeserializer.class);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = cf.createConsumer();
        if(attachToBroker){
            kafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);
        }
        return consumer;
    }

    protected void readFromEmbeddedKafka(String topic, String groupId, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, Integer expectedMessagesCount, Duration timeout) {
        readFromEmbeddedKafka(getEmbeddedKafkaConsumer(topic, groupId), consumeMessage, true, expectedMessagesCount, timeout);
    }

    protected void readFromEmbeddedKafka(Consumer<String, String> consumer, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, boolean consumeFromBeginning, Integer expectedMessagesCount, Duration timeout) {
        if (consumeFromBeginning) {
            consumeFromBeginning(consumer);
        }
        int i = 0;
        while (i < expectedMessagesCount) {
            ConsumerRecords<String, String> published = consumer.poll(timeout);
            for (ConsumerRecord<String, String> stringStringConsumerRecord : published) {
                consumeMessage.accept(stringStringConsumerRecord);
                i++;
            }
        }
    }

    protected void consumeFromBeginning(Consumer<String, String> consumer) {
        consumer.seekToBeginning(consumer.assignment());
    }

    protected List<ConsumerRecord<String, String>> consumeMessages(String topic, int expectedNumber, long maxWaitingMs) {
        return consumeMessages(topic, "idpay-group-test-check", expectedNumber, maxWaitingMs);
    }

    protected List<ConsumerRecord<String, String>> consumeMessages(String topic, String groupId, int expectedNumber, long maxWaitingMs) {
        long startTime = System.currentTimeMillis();
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, groupId)) {

            List<ConsumerRecord<String, String>> payloadConsumed = new ArrayList<>(expectedNumber);
            while (payloadConsumed.size() < expectedNumber) {
                if (System.currentTimeMillis() - startTime > maxWaitingMs) {
                    Assertions.fail("timeout of %d ms expired. Read %d messages of %d".formatted(maxWaitingMs, payloadConsumed.size(), expectedNumber));
                }
                consumer.poll(Duration.ofMillis(7000)).iterator().forEachRemaining(payloadConsumed::add);
            }
            return payloadConsumed;
        }
    }

    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, Object payload) {
        try {
            publishIntoEmbeddedKafka(topic, headers, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private int totaleMessageSentCounter =0;
    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, String payload) {
        publishIntoEmbeddedKafka(topic, null, headers, key, payload);
    }
    protected void publishIntoEmbeddedKafka(String topic, Integer partition, Iterable<Header> headers, String key, String payload) {
        final RecordHeader dummyHeader = new RecordHeader("DUMMY", "VALUE".getBytes(StandardCharsets.UTF_8));

        final RecordHeader retryHeader = new RecordHeader("retry", "1".getBytes(StandardCharsets.UTF_8));
        final RecordHeader applicationNameHeader = new RecordHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, APPLICATION_NAME.getBytes(StandardCharsets.UTF_8));

        AtomicBoolean containAppNameHeader = new AtomicBoolean(false);
        if(headers!= null){
            headers.forEach(h -> {
                if(h.key().equals(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME)){
                    containAppNameHeader.set(true);
                }
            });
        }

        final RecordHeader[] additionalHeaders;
        if(totaleMessageSentCounter++%2 == 0 || containAppNameHeader.get()){
            additionalHeaders= new RecordHeader[]{dummyHeader};
        } else {
            additionalHeaders= new RecordHeader[]{dummyHeader, retryHeader, applicationNameHeader};
        }

        if (headers == null) {
            headers = new RecordHeaders(additionalHeaders);
        } else {
            headers = Stream.concat(
                            StreamSupport.stream(headers.spliterator(), false),
                            Arrays.stream(additionalHeaders))
                    .collect(Collectors.toList());
        }
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, partition, key, payload, headers);
        template.send(record);
    }

    protected Map<TopicPartition, OffsetAndMetadata> getCommittedOffsets(String topic, String groupId){
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, groupId, false)) {
            return consumer.committed(consumer.partitionsFor(topic).stream().map(p-> new TopicPartition(topic, p.partition())).collect(Collectors.toSet()));
        }
    }

    protected Map<TopicPartition, OffsetAndMetadata> checkCommittedOffsets(String topic, String groupId, long expectedCommittedMessages){
        return checkCommittedOffsets(topic, groupId, expectedCommittedMessages, 10, 500);
    }

    // Cannot use directly Awaitlity cause the Callable condition is performed on separate thread, which will go into conflict with the consumer Kafka access
    protected Map<TopicPartition, OffsetAndMetadata> checkCommittedOffsets(String topic, String groupId, long expectedCommittedMessages, int maxAttempts, int millisAttemptDelay){
        RuntimeException lastException = null;
        if(maxAttempts<=0){
            maxAttempts=1;
        }

        for(;maxAttempts>0; maxAttempts--){
            try {
                final Map<TopicPartition, OffsetAndMetadata> commits = getCommittedOffsets(topic, groupId);
                Assertions.assertEquals(expectedCommittedMessages, commits.values().stream().mapToLong(OffsetAndMetadata::offset).sum());
                return commits;
            } catch (Throwable e){
                lastException = new RuntimeException(e);
                wait(millisAttemptDelay, TimeUnit.MILLISECONDS);
            }
        }
        throw lastException;
    }

    protected Map<TopicPartition, Long> getEndOffsets(String topic){
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, "idpay-group-test-check", false)) {
            return consumer.endOffsets(consumer.partitionsFor(topic).stream().map(p-> new TopicPartition(topic, p.partition())).toList());
        }
    }

    protected Map<TopicPartition, Long> checkPublishedOffsets(String topic, long expectedPublishedMessages){
        Map<TopicPartition, Long> endOffsets = getEndOffsets(topic);
        Assertions.assertEquals(expectedPublishedMessages, endOffsets.values().stream().mapToLong(x->x).sum());
        return endOffsets;
    }

    protected static void waitFor(Callable<Boolean> test, Supplier<String> buildTestFailureMessage, int maxAttempts, int millisAttemptDelay) {
        try {
            await()
                    .pollInterval(millisAttemptDelay, TimeUnit.MILLISECONDS)
                    .atMost((long) maxAttempts * millisAttemptDelay, TimeUnit.MILLISECONDS)
                    .until(test);
        } catch (RuntimeException e) {
            Assertions.fail(buildTestFailureMessage.get(), e);
        }
    }

    protected static void wait(long timeout, TimeUnit timeoutUnit) {
        try{
            Awaitility.await().atLeast(timeout, timeoutUnit).until(()->false);
        } catch (ConditionTimeoutException ex){
            // Do Nothing
        }
    }

    protected final Pattern errorUseCaseIdPatternMatch = getErrorUseCaseIdPatternMatch();

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"initiativeId\":\"id_([0-9]+)_?[^\"]*\"");
    }

    protected void checkErrorsPublished(int expectedErrorMessagesNumber, long maxWaitingMs, List<Pair<Supplier<String>, java.util.function.Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
        final List<ConsumerRecord<String, String>> errors = consumeMessages(topicErrors, expectedErrorMessagesNumber, maxWaitingMs);
        for (final ConsumerRecord<String, String> record : errors) {
            final Matcher matcher = errorUseCaseIdPatternMatch.matcher(record.value());
            int useCaseId = matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
            if (useCaseId == -1) {
                throw new IllegalStateException("UseCaseId not recognized! " + record.value());
            }
            errorUseCases.get(useCaseId).getSecond().accept(record);
        }
    }

    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, boolean expectedRetriable, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedRetriable, expectedPayload, expectedKey, true, true);
    }
    protected void checkErrorMessageHeaders(String srcTopic, String group, ConsumerRecord<String, String> errorMessage, String errorDescription, boolean expectedRetriable, String expectedPayload, String expectedKey, boolean expectDummyHeader, boolean expectedAppNameHeader) {
        if(expectedAppNameHeader) {
            Assertions.assertEquals(APPLICATION_NAME, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME));
        }
        Assertions.assertEquals(group, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_GROUP));
        Assertions.assertEquals("kafka", TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_TYPE));
        Assertions.assertEquals(bootstrapServers, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_SERVER));
        Assertions.assertEquals(srcTopic, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_TOPIC));
        Assertions.assertNotNull(errorMessage.headers().lastHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_STACKTRACE));
        Assertions.assertEquals(errorDescription, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_DESCRIPTION));
        Assertions.assertEquals(expectedRetriable+"", TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_RETRYABLE));
        if(expectDummyHeader){
            Assertions.assertEquals("VALUE", TestUtils.getHeaderValue(errorMessage, "DUMMY")); // to test if headers are correctly propagated
        }
        Assertions.assertEquals(expectedPayload, errorMessage.value());
        Assertions.assertEquals(expectedKey, errorMessage.key());
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
    protected int getExpectedTrxsCount(int validMsgs) {
        int zeroBasedFix = validMsgs % 3 == 0 ? 0 : 1; // because 0 based, we have to add 1, but if using a multiple of 3, we have to remove one, compensating the 0 based bias
        return validMsgs
                - (validMsgs / 6 * 2 + zeroBasedFix) // each %6 will be a complete refund
                - (validMsgs / 3 - validMsgs / 6 + zeroBasedFix); // each %3 will be a refund
    }

    @Autowired
    protected InitiativeStatRepository initiativeStatRepository;
    protected long waitForCounterResult(String initiativeId, String organizationId, Function<InitiativeStatistics, Long> getterCounter, long expectedCounterValue, long maxWaitingMs) {
        int millisAttemptDelay = 500;
        int maxAttempts = (int) maxWaitingMs / millisAttemptDelay;

        long[] countSaved = {0};
        waitFor(() -> (countSaved[0] = initiativeStatRepository.findById(initiativeId)
                        .filter(r-> {
                            Assertions.assertEquals(organizationId, r.getOrganizationId());
                            return true;
                        })
                        .map(getterCounter).orElse(-1L)) >= expectedCounterValue
                , () -> "Expected %d counter value for initiative %s, read %d".formatted(expectedCounterValue, initiativeId, countSaved[0])
                , maxAttempts, millisAttemptDelay);
        return countSaved[0];
    }

    protected long verifyPartitionOffsetStored(long expectOffsetSum, String initiativeid, Function<InitiativeStatistics, List<InitiativeStatistics.CommittedOffset>> getterStatisticsCommittedOffsets, boolean assertEquals) {
        InitiativeStatistics result = initiativeStatRepository.findById(initiativeid).orElse(null);
        Assertions.assertNotNull(result);

        // -2 because offset start from 0 and we are using 2 partition for test
        long expectedOffsetSum0Based = expectOffsetSum - 2;
        long sum = getterStatisticsCommittedOffsets.apply(result).stream().mapToLong(InitiativeStatistics.CommittedOffset::getOffset).sum();

        if (assertEquals) {
            Assertions.assertEquals(expectedOffsetSum0Based, sum);
        } else {
            Assertions.assertTrue(expectedOffsetSum0Based>=sum, "Expected at least %d obtained %d".formatted(expectedOffsetSum0Based, sum));
        }

        return sum + 2;
    }
    //endregion
}
