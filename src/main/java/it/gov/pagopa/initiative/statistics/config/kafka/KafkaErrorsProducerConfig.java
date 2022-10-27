package it.gov.pagopa.initiative.statistics.config.kafka;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
@EnableKafka
@ConfigurationProperties("app.kafka.producer.errors")
public class KafkaErrorsProducerConfig {

    @Setter
    private String topic;
    @Setter
    private Map<String, Object> producerProperties;

    @Bean
    @Qualifier("errors")
    public KafkaTemplate<String, String> errorsMessagePublisher(ProducerFactory<String, String> producerFactory) {
            KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory, producerProperties);
            kafkaTemplate.setDefaultTopic(topic);
            return kafkaTemplate;
    }
}
