package it.gov.pagopa.initiative.statistics.config.kafka;

import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
abstract class BaseKafkaConsumerConfig extends KafkaProperties.Consumer {

    @Setter
    protected String topic;
    @Setter
    protected KafkaProperties.Listener listener;

    protected ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(buildConsumerFactory(consumerFactory));
        factory.setConcurrency(listener.getConcurrency());
        factory.setBatchListener(KafkaProperties.Listener.Type.BATCH.equals(listener.getType()));
        configureContainerProperties(factory.getContainerProperties());
        return factory;
    }

    private ConsumerFactory<String, String> buildConsumerFactory(ConsumerFactory<String, String> consumerFactory){
        Map<String, Object> props = new HashMap<>(consumerFactory.getConfigurationProperties());
        props.putAll(this.buildProperties());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    private void configureContainerProperties(ContainerProperties containerProperties) {
        containerProperties.setAckMode(listener.getAckMode());
        containerProperties.setAckCount(listener.getAckCount());
        containerProperties.setAckTime(listener.getAckTime().toMillis());
    }

}
