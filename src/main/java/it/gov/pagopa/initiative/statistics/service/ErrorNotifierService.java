package it.gov.pagopa.initiative.statistics.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface ErrorNotifierService {
    void notifyOnboardingOutcome(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception);
    void notifyTransactionEvaluation(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception);
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    void notify(String srcType, String srcServer, String srcTopic, String group, ConsumerRecord<String, String> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}