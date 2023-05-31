package it.gov.pagopa.initiative.statistics.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class StatisticsErrorNotifierServiceImpl implements StatisticsErrorNotifierService {

    private final ErrorNotifierService errorNotifierService;

    private final String onboardingOutcomeMessagingServiceType;
    private final String onboardingOutcomeServer;
    private final String onboardingOutcomeTopic;
    private final String onboardingOutcomeGroup;

    private final String transactionEvaluationMessagingServiceType;
    private final String transactionEvaluationServer;
    private final String transactionEvaluationTopic;
    private final String transactionEvaluationGroup;

    private final String merchantCountersTransactionMessagingServiceType;
    private final String merchantCountersTransactionServer;
    private final String merchantCountersTransactionTopic;
    private final String merchantCountersTransactionGroup;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public StatisticsErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,

                                              @Value("kafka") String onboardingOutcomeMessagingServiceType,
                                              @Value("${app.kafka.consumer.onboarding-outcome.bootstrap-servers}") String onboardingOutcomeServer,
                                              @Value("${app.kafka.consumer.onboarding-outcome.topic}") String onboardingOutcomeTopic,
                                              @Value("${app.kafka.consumer.onboarding-outcome.group-id}") String onboardingOutcomeGroup,

                                              @Value("kafka") String transactionEvaluationMessagingServiceType,
                                              @Value("${app.kafka.consumer.transaction-evaluation.bootstrap-servers}") String transactionEvaluationServer,
                                              @Value("${app.kafka.consumer.transaction-evaluation.topic}") String transactionEvaluationTopic,
                                              @Value("${app.kafka.consumer.transaction-evaluation.group-id}") String transactionEvaluationGroup,

                                              @Value("kafka") String merchantCountersTransactionMessagingServiceType,
                                              @Value("${app.kafka.consumer.merchant-counters-transaction.bootstrap-servers}") String merchantCountersTransactionServer,
                                              @Value("${app.kafka.consumer.merchant-counters-transaction.topic}") String merchantCountersTransactionTopic,
                                              @Value("${app.kafka.consumer.merchant-counters-transaction.group-id}") String merchantCountersTransactionGroup
    ) {
        this.errorNotifierService = errorNotifierService;

        this.onboardingOutcomeMessagingServiceType = onboardingOutcomeMessagingServiceType;
        this.onboardingOutcomeServer = onboardingOutcomeServer;
        this.onboardingOutcomeTopic = onboardingOutcomeTopic;
        this.onboardingOutcomeGroup = onboardingOutcomeGroup;

        this.transactionEvaluationMessagingServiceType = transactionEvaluationMessagingServiceType;
        this.transactionEvaluationServer = transactionEvaluationServer;
        this.transactionEvaluationTopic = transactionEvaluationTopic;
        this.transactionEvaluationGroup = transactionEvaluationGroup;

        this.merchantCountersTransactionMessagingServiceType = merchantCountersTransactionMessagingServiceType;
        this.merchantCountersTransactionServer = merchantCountersTransactionServer;
        this.merchantCountersTransactionTopic = merchantCountersTransactionTopic;
        this.merchantCountersTransactionGroup = merchantCountersTransactionGroup;
    }

    @Override
    public void notifyOnboardingOutcome(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception) {
        notify(onboardingOutcomeMessagingServiceType, onboardingOutcomeServer, onboardingOutcomeTopic, onboardingOutcomeGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notifyTransactionEvaluation(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception) {
        notify(transactionEvaluationMessagingServiceType, transactionEvaluationServer, transactionEvaluationTopic, transactionEvaluationGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notifyMerchantCountersTransaction(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception) {
        notify(merchantCountersTransactionMessagingServiceType, merchantCountersTransactionServer, merchantCountersTransactionTopic, merchantCountersTransactionGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notify(String srcType, String srcServer, String srcTopic, String group, ConsumerRecord<String, String> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        errorNotifierService.notify(srcType, srcServer, srcTopic, group, convertMessage(message), description, retryable,resendApplication, exception);
    }

    private static Message<String> convertMessage(ConsumerRecord<String, String> message) {
        return MessageBuilder.createMessage(message.value(), convertHeaders(message));
    }

    private static MessageHeaders convertHeaders(ConsumerRecord<String, String> message) {
        return new MessageHeaders(
                Stream.concat(
                                StreamSupport.stream(message.headers().spliterator(), false),
                                message.key() != null ? Stream.of(new RecordHeader(KafkaHeaders.RECEIVED_MESSAGE_KEY, message.key().getBytes(StandardCharsets.UTF_8))) : Stream.empty()
                        )
                        .collect(Collectors.toMap(
                                Header::key,
                                Header::value)
                        ));
    }
}
