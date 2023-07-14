package it.gov.pagopa.initiative.statistics.events.producers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@ExtendWith(MockitoExtension.class)
class StatistiscsErrorPublisherTest {

    @Mock
    private KafkaTemplate<String, String> publisherMock;
    private StatistiscsErrorPublisher statistiscsErrorPublisher;

    @BeforeEach
    void setUp() {
        statistiscsErrorPublisher = new StatistiscsErrorPublisher(publisherMock);
    }

    @Test
    void send_exceptionally() {
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(KafkaHeaders.TOPIC, "topic")
                .build();

        Mockito.when(publisherMock.send(Mockito.any(Message.class))).thenReturn(CompletableFuture.failedFuture(new Throwable()));

        try {
            statistiscsErrorPublisher.send(message);
        } catch (CompletionException e) {
        }

        Assertions.fail();
    }
}
