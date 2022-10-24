package it.gov.pagopa.initiative.statistics.config.kafka;

import it.gov.pagopa.initiative.statistics.events.consumers.TransactionEvaluationMessagesListener;
import it.gov.pagopa.initiative.statistics.service.trx.TransactionEvaluationStatisticsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@ConfigurationProperties("app.kafka.consumer.transaction-evaluation")
public class KafkaTransactionEvaluationConsumerConfig extends BaseKafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaTransactionEvaluationListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        return super.kafkaListenerContainerFactory(consumerFactory);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, String> kafkaTransactionEvaluationContainer(
            @Qualifier("kafkaTransactionEvaluationListenerContainerFactory") ConcurrentKafkaListenerContainerFactory<String, String> factory,
            TransactionEvaluationStatisticsService transactionEvaluationStatisticsService) {
        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer(topic);
        container.getContainerProperties().setMessageListener(new TransactionEvaluationMessagesListener(transactionEvaluationStatisticsService));
        return container;
    }
}
