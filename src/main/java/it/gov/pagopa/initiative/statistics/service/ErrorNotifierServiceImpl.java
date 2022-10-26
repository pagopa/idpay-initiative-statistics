package it.gov.pagopa.initiative.statistics.service;

import it.gov.pagopa.initiative.statistics.events.producers.ErrorPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {
    public static final String ERROR_MSG_HEADER_APPLICATION_NAME = "applicationName";
    public static final String ERROR_MSG_HEADER_GROUP = "group";

    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";

    private final ErrorPublisher errorPublisher;
    private final String applicationName;

    private final String onboardingOutcomeMessagingServiceType;
    private final String onboardingOutcomeServer;
    private final String onboardingOutcomeTopic;
    private final String onboardingOutcomeGroup;

    private final String transactionEvaluationMessagingServiceType;
    private final String transactionEvaluationServer;
    private final String transactionEvaluationTopic;
    private final String transactionEvaluationGroup;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public ErrorNotifierServiceImpl(ErrorPublisher errorPublisher,
                                    @Value("${spring.application.name}") String applicationName,

                                    @Value("kafka") String onboardingOutcomeMessagingServiceType,
                                    @Value("${app.kafka.consumer.onboarding-outcome.bootstrap-servers}") String onboardingOutcomeServer,
                                    @Value("${app.kafka.consumer.onboarding-outcome.topic}") String onboardingOutcomeTopic,
                                    @Value("${app.kafka.consumer.onboarding-outcome.group-id}") String onboardingOutcomeGroup,

                                    @Value("kafka") String transactionEvaluationMessagingServiceType,
                                    @Value("${app.kafka.consumer.onboarding-outcome.bootstrap-servers}") String transactionEvaluationServer,
                                    @Value("${app.kafka.consumer.onboarding-outcome.topic}") String transactionEvaluationTopic,
                                    @Value("${app.kafka.consumer.onboarding-outcome.group-id}") String transactionEvaluationGroup) {
        this.errorPublisher = errorPublisher;
        this.applicationName = applicationName;

        this.onboardingOutcomeMessagingServiceType = onboardingOutcomeMessagingServiceType;
        this.onboardingOutcomeServer = onboardingOutcomeServer;
        this.onboardingOutcomeTopic = onboardingOutcomeTopic;
        this.onboardingOutcomeGroup = onboardingOutcomeGroup;

        this.transactionEvaluationMessagingServiceType = transactionEvaluationMessagingServiceType;
        this.transactionEvaluationServer = transactionEvaluationServer;
        this.transactionEvaluationTopic = transactionEvaluationTopic;
        this.transactionEvaluationGroup = transactionEvaluationGroup;
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
    public void notify(String srcType, String srcServer, String srcTopic, String group, ConsumerRecord<String, String> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", description, exception);
        final MessageBuilder<String> errorMessage = MessageBuilder.withPayload(message.value())
                .copyHeaders(getRecordHeaders(message))
                .setHeader(ERROR_MSG_HEADER_GROUP, group)
                .setHeader(ERROR_MSG_HEADER_SRC_TYPE, srcType)
                .setHeader(ERROR_MSG_HEADER_SRC_SERVER, srcServer)
                .setHeader(ERROR_MSG_HEADER_SRC_TOPIC, srcTopic)
                .setHeader(ERROR_MSG_HEADER_DESCRIPTION, description)
                .setHeader(ERROR_MSG_HEADER_RETRYABLE, retryable)
                .setHeader(ERROR_MSG_HEADER_STACKTRACE, ExceptionUtils.getStackTrace(exception));

        addExceptionInfo(errorMessage, "rootCause", ExceptionUtils.getRootCause(exception));
        addExceptionInfo(errorMessage, "cause", exception.getCause());

        if (resendApplication){
            errorMessage.setHeader(ERROR_MSG_HEADER_APPLICATION_NAME, applicationName);
        }

        String receivedKey = message.key();
        if(receivedKey!=null){
            errorMessage.setHeader(KafkaHeaders.MESSAGE_KEY, receivedKey);
        }

        errorPublisher.send(errorMessage.build());
    }

    private static Map<String, String> getRecordHeaders(ConsumerRecord<String, String> message) {
        return StreamSupport.stream(message.headers().spliterator(), false)
                .collect(Collectors.toMap(Header::key, r->new String(r.value(), StandardCharsets.UTF_8)));
    }

    private void addExceptionInfo(MessageBuilder<?> errorMessage, String exceptionHeaderPrefix, Throwable rootCause) {
        errorMessage
                .setHeader("%sClass".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getClass().getName() : null)
                .setHeader("%sMessage".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getMessage() : null);
    }
}
