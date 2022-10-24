package it.gov.pagopa.initiative.statistics.service;

public interface ErrorNotifierService {
    void notifyOnboardingOutcome(String description, boolean retryable, Throwable exception);
    void notifyTransactionEvaluation(String description, boolean retryable, Throwable exception);
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    void notify(String srcType, String srcServer, String srcTopic, String group, String description, boolean retryable, boolean resendApplication, Throwable exception);
}