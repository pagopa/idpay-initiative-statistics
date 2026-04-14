package it.gov.pagopa.initiative.statistics.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsErrorNotifierServiceImplTest {

    @Mock
    private ErrorNotifierService errorNotifierService;

    @InjectMocks
    private StatisticsErrorNotifierServiceImpl service;

    @Test
    void shouldDelegateNotifyOnboardingOutcome() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L, "key1", "value1");

        service.notifyOnboardingOutcome(record, "desc", true, new RuntimeException("err"));

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);

        verify(errorNotifierService, times(1))
                .notify(any(), any(), any(), any(), messageCaptor.capture(),
                        eq("desc"), eq(true), eq(true), any());

        Message<String> message = messageCaptor.getValue();

        assertEquals("value1", message.getPayload());
    }

    @Test
    void shouldDelegateNotifyTransactionEvaluation() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 1, 1L, "key2", "value2");

        service.notifyTransactionEvaluation(record, "desc", false, null);

        verify(errorNotifierService, times(1))
                .notify(any(), any(), any(), any(), any(),
                        eq("desc"), eq(false), eq(true), any());
    }

    @Test
    void shouldConvertHeadersAndPayloadCorrectly() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 2, 2L, "k", "v");

        service.notifyCommandsOperation(record, "test", true, null);

        ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);

        verify(errorNotifierService).notify(
                any(), any(), any(), any(),
                captor.capture(),
                eq("test"),
                eq(true),
                eq(true),
                any()
        );

        Message<String> msg = captor.getValue();

        assertEquals("v", msg.getPayload());
    }

    @Test
    void shouldHandleNullKey() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L, null, "value");

        service.notifyMerchantCountersTransaction(record, "desc", true, null);

        ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);

        verify(errorNotifierService).notify(
                any(), any(), any(), any(),
                captor.capture(),
                anyString(),
                anyBoolean(),
                anyBoolean(),
                any()
        );

        Message<String> msg = captor.getValue();

        assertEquals("value", msg.getPayload());
        assertFalse(msg.getHeaders().containsKey("kafka_receivedKey"));
    }
}