package it.gov.pagopa.initiative.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.kafka.KafkaTestUtilitiesService;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.utils.TestIntegrationUtils;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(topics = {
        "${app.kafka.producer.errors.topic}",
        "${app.kafka.consumer.onboarding-outcome.topic}",
        "${app.kafka.consumer.transaction-evaluation.topic}",
        "merchant-counters-transaction", // "${app.kafka.consumer.merchant-counters-transaction.topic}", Overridden in TestPropertySource
        "${app.kafka.consumer.merchant-counters-reward-notification.topic}",
        "${app.kafka.consumer.commands.topic}",
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
                "app.kafka.consumer.commands.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "app.kafka.consumer.commands.security.protocol=PLAINTEXT",
                //endregion

                //region kafka test overrides
                "app.kafka.consumer.merchant-counters-transaction.topic=merchant-counters-transaction",
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.de.flapdoodle.embed.mongo.spring.autoconfigure=WARN",
                "de.flapdoodle.mongodb.embedded.version=4.0.21",
                //endregion

                //region delete
                "app.delete.paginationSize=100",
                "app.delete.delayTime=1000"
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
    @Value("${app.kafka.consumer.commands.topic}")
    protected String topicCommands;

    @Value("${app.kafka.consumer.onboarding-outcome.group-id}")
    protected String groupIdOnboardingOutcome;
    @Value("${app.kafka.consumer.transaction-evaluation.group-id}")
    protected String groupIdTransactionEvaluation;
    @Value("${app.kafka.consumer.merchant-counters-transaction.group-id}")
    protected String groupIdMerchantCountersTransaction;
    @Value("${app.kafka.consumer.merchant-counters-reward-notification.group-id}")
    protected String groupIdMerchantCountersNotification;
    @Value("${app.kafka.consumer.commands.group-id}")
    protected String groupIdCommands;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TestIntegrationUtils.setDefaultTimeZoneAndUnregisterCommonMBean();
    }

    @PostConstruct
    public void logEmbeddedServerConfig() {
        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Embedded kafka: %s
                        ************************
                        """,
            mongoTestUtilitiesService.getMongoUrl(),
            kafkaTestUtilitiesService.getKafkaUrls());
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
}
