package it.gov.pagopa.initiative.statistics.events.producers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.utils.MemoryAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
class StatistiscsErrorPublisherTest {

    @Mock
    private KafkaTemplate<String, String> publisherMock;
    private StatistiscsErrorPublisher statistiscsErrorPublisher;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        statistiscsErrorPublisher = new StatistiscsErrorPublisher(publisherMock);

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(StatistiscsErrorPublisher.class.getName());
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @Test
    void send_exceptionally() {
        Message<String> message = MessageBuilder.withPayload("test")
                .setHeader(KafkaHeaders.TOPIC, "topic")
                .build();

        Mockito.when(publisherMock.send(Mockito.any(Message.class))).thenReturn(CompletableFuture.failedFuture(new Throwable()));

        statistiscsErrorPublisher.send(message);
        Assertions.assertEquals(1, memoryAppender.getLoggedEvents().size());
        Assertions.assertEquals(
                String.format(
                        "[ERROR_MESSAGE_HANDLER] something gone wrong while sending message towards topic %s",
                        publisherMock.getDefaultTopic()
                ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage());
        Assertions.assertEquals(Level.ERROR, memoryAppender.getLoggedEvents().get(0).getLevel());
    }
}
