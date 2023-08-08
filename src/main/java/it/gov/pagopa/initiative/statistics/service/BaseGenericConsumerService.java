package it.gov.pagopa.initiative.statistics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.io.IOException;
import java.util.List;

@Slf4j
public abstract class BaseGenericConsumerService<E> extends BaseKafkaConsumer<E>{
    protected BaseGenericConsumerService(String applicationName,
                                         String consumerGroup,
                                         ObjectMapper objectMapper) {
        super(applicationName, consumerGroup, objectMapper);
    }

    public void evaluate(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment, Consumer<?, ?> consumer){
        log.debug("[{}] Evaluating {} records", getFlowName(), records.size());

        for (ConsumerRecord<String, String> r : records) {
            try {
                this.evaluateRecords(r, consumer);
            } catch (Exception e) {
                log.error(String.format("Something gone wrong during the evaluation of the payload:%n%s", r.value()), e);
                this.onRecordError2notify(r,"[%s] Something gone wrong during the evaluation of the payload: %s".formatted(getFlowName(), r.value()), e);
            }
        }

        if(acknowledgment!=null){
            acknowledgment.acknowledge();
        }
    }

    /** It will evaluate record*/
    @SuppressWarnings("java:S3864") // suppressing peek warning: in this case the optimization described will not be performed
    private void evaluateRecords(ConsumerRecord<String, String> consumerRecord, Consumer<?, ?> consumer){
        log.debug("[{}] Evaluating record", getFlowName());

        Pair<ConsumerRecord<String, String>, E> record2Payload = null;

        try {
            record2Payload = Pair.of(consumerRecord, deserialize(consumerRecord.value()));
        } catch (IOException e) {
            onDeserializeError(consumerRecord,
                    "[%s] Unexpected json: %s".formatted(getFlowName(), consumerRecord.value()),
                    e);
        }

        if(record2Payload != null && this.isNotRetry(record2Payload)) {
            evaluate(record2Payload.getRight());
        }

    }

    /** The name of the business logic flow to print when logging */
    protected abstract String getFlowName();

    /** In case of errors deserialize a message */
    protected abstract void onDeserializeError(ConsumerRecord<String, String> message, String description, Throwable exception);

    protected abstract void evaluate(E payload);
}
