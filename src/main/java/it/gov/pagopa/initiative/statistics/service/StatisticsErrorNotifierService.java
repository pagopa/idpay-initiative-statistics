package it.gov.pagopa.initiative.statistics.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface StatisticsErrorNotifierService {
    void notifyOnboardingOutcome(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception);
    void notifyTransactionEvaluation(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception);
    void notifyMerchantCountersTransaction(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception);
    void notifyMerchantCountersRewardNotification(ConsumerRecord<String, String> message, String description, boolean retryable, Throwable exception);
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    void notify(String srcType, String srcServer, String srcTopic, String group, ConsumerRecord<String, String> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}