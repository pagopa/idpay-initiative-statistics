package it.gov.pagopa.initiative.statistics.service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseGenericConsumerServiceTest {

    @Mock
    private ObjectMapper objectMapperMock;
    @Mock
    private Acknowledgment acknowledgmentMock;
    @Mock
    private Consumer<String, String> kafkaConsumerMock;

    private BaseGenericConsumerServiceStub service;

    private static class BaseGenericConsumerServiceStub extends BaseGenericConsumerService<String> {
        protected BaseGenericConsumerServiceStub(ObjectMapper objectMapper) {
            super("appName", "consumerGroup", objectMapper);
        }

        @Override
        protected void onDeserializeError(ConsumerRecord<String, String> message, String description, Throwable exception) {
            //empty
        }

        @Override
        protected void evaluate(String payload) {
            //empty
        }

        @Override
        protected String getFlowName() {
            return "TEST_FLOW";
        }

        @Override
        protected String deserialize(String value) throws JacksonException {
            return value;
        }

        @Override
        protected boolean isNotRetry(ConsumerRecord<String, String> consumerRecord) {
            return true;
        }

        @Override
        protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
            //
        }

        @Override
        protected Class<String> getRecordClass(){
            return null;
        }

    }

    @BeforeEach
    void setUp() {
        // Creiamo uno spy sulla classe stub per verificare le chiamate ai metodi astratti
        service = spy(new BaseGenericConsumerServiceStub(objectMapperMock));
    }

    @Test
    void evaluate_shouldProcessRecordsAndAcknowledge() {
        // Given
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", "payload");
        List<ConsumerRecord<String, String>> records = List.of(consumerRecord);

        // When
        service.evaluate(records, acknowledgmentMock, kafkaConsumerMock);

        // Then
        verify(service, times(1)).evaluate("payload");
        verify(acknowledgmentMock, times(1)).acknowledge();
    }

    @Test
    void evaluate_shouldHandleDeserializationError() throws JacksonException {
        // Given
        String invalidJson = "invalid";
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", invalidJson);

        // Forziamo l'errore di deserializzazione
        doThrow(mock(JacksonException.class)).when(service).deserialize(invalidJson);

        // When
        service.evaluate(List.of(consumerRecord), acknowledgmentMock, kafkaConsumerMock);

        // Then
        verify(service, times(1)).onDeserializeError(eq(consumerRecord), anyString(), any(JacksonException.class));
        verify(acknowledgmentMock, times(1)).acknowledge();
    }

    @Test
    void evaluate_shouldHandleGenericProcessingError() {
        // Given
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", "payload");
        doThrow(new RuntimeException("Error")).when(service).evaluate("payload");

        // When
        service.evaluate(List.of(consumerRecord), acknowledgmentMock, kafkaConsumerMock);

        // Then
        verify(service, times(1)).onRecordError2notify(eq(consumerRecord), anyString(), any(RuntimeException.class));
        verify(acknowledgmentMock, times(1)).acknowledge();
    }

    @Test
    void evaluate_shouldNotAcknowledgeIfNull() {
        // Given
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", "payload");

        // When
        service.evaluate(List.of(consumerRecord), null, kafkaConsumerMock);

        // Then
        verify(service, times(1)).evaluate("payload");
        // Nessuna eccezione e test passa senza chiamare acknowledgmentMock
    }
}