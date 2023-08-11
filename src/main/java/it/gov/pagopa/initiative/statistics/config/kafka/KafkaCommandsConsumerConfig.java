package it.gov.pagopa.initiative.statistics.config.kafka;

import it.gov.pagopa.initiative.statistics.events.consumers.CommandsMessagesListener;
import it.gov.pagopa.initiative.statistics.service.commands.CommandsMediatorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
@ConfigurationProperties("app.kafka.consumer.commands")
public class KafkaCommandsConsumerConfig extends BaseKafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaCommandsListenerContainerFactory(ConsumerFactory<String, String> consumerFactory, KafkaProperties defaultKafkaProperties) {
        return super.kafkaListenerContainerFactory(consumerFactory, defaultKafkaProperties);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, String> kafkaCommandsContainer(
            @Qualifier("kafkaCommandsListenerContainerFactory") ConcurrentKafkaListenerContainerFactory<String, String> factory,
            CommandsMediatorService commandsMediatorService) {
        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer(topic);
        container.getContainerProperties().setMessageListener(new CommandsMessagesListener(commandsMediatorService));
        return container;
    }
}
