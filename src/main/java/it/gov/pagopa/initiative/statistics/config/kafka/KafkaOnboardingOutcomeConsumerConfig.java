package it.gov.pagopa.initiative.statistics.config.kafka;

import it.gov.pagopa.initiative.statistics.events.consumers.OnboardingOutcomeMessagesListener;
import it.gov.pagopa.initiative.statistics.service.onboarding.OnboardingStatisticsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@ConfigurationProperties("app.kafka.consumer.onboarding-outcome")
public class KafkaOnboardingOutcomeConsumerConfig extends BaseKafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaOnboardingOutcomeListenerContainerFactory(ConsumerFactory<String, String> consumerFactory, KafkaProperties defaultKafkaProperties) {
        return super.kafkaListenerContainerFactory(consumerFactory, defaultKafkaProperties);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, String> kafkaOnboardingOutcomeContainer(
            @Qualifier("kafkaOnboardingOutcomeListenerContainerFactory") ConcurrentKafkaListenerContainerFactory<String, String> factory,
            OnboardingStatisticsService onboardingStatisticsService) {
        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer(topic);
        container.getContainerProperties().setMessageListener(new OnboardingOutcomeMessagesListener(onboardingStatisticsService));
        return container;
    }
}
