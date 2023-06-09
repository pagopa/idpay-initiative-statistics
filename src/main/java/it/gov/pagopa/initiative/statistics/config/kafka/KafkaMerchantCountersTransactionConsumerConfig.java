package it.gov.pagopa.initiative.statistics.config.kafka;

import it.gov.pagopa.initiative.statistics.events.consumers.MerchantCountersTransactionMessagesListener;
import it.gov.pagopa.initiative.statistics.service.merchant.counters.trx.MerchantTransactionStatisticsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@ConfigurationProperties("app.kafka.consumer.merchant-counters-transaction")
public class KafkaMerchantCountersTransactionConsumerConfig extends BaseKafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaMerchantTransactionListenerContainerFactory(ConsumerFactory<String, String> consumerFactory, KafkaProperties defaultKafkaProperties) {
        return super.kafkaListenerContainerFactory(consumerFactory, defaultKafkaProperties);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, String> kafkaMerchantTransactionContainer(
            @Qualifier("kafkaMerchantTransactionListenerContainerFactory") ConcurrentKafkaListenerContainerFactory<String, String> factory,
            MerchantTransactionStatisticsService merchantTransactionStatisticsService) {
        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer(topic);
        container.getContainerProperties().setMessageListener(new MerchantCountersTransactionMessagesListener(merchantTransactionStatisticsService));
        return container;
    }
}
