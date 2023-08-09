package it.gov.pagopa.initiative.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class KafkaConsumerUtilities<E> {
    protected final String applicationName;
    protected final String consumerGroup;
    protected final ObjectReader objectReader;

    protected KafkaConsumerUtilities(String applicationName, String consumerGroup, ObjectMapper objectMapper) {
        this.applicationName = applicationName;
        this.consumerGroup = consumerGroup;
        this.objectReader = objectMapper.readerFor(getRecordClass());
    }

    protected abstract Class<E> getRecordClass();


    /** It will check if the current record is not a RETRY of another application */
    protected boolean isNotRetry(ConsumerRecord<String, String> consumerRecord) {

        Header appNameRecord = consumerRecord.headers().lastHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME);
        Header retry = consumerRecord.headers().lastHeader(KafkaConstants.ERROR_MSG_HEADER_RETRY);
        Header group = consumerRecord.headers().lastHeader(KafkaConstants.ERROR_MSG_HEADER_GROUP);
        boolean isSameGroup = group == null || new String(group.value(), StandardCharsets.UTF_8).equals(consumerGroup);
        boolean out = retry == null || (appNameRecord != null && applicationName.equals(new String(appNameRecord.value(), StandardCharsets.UTF_8)) && isSameGroup);
        if(!out){
            log.info("[INITIATIVE_STATISTICS_EVALUATION][{}] Skipping record because other application retry: appName: {}, retry: {}"
                    , getFlowName(),
                    appNameRecord!=null? new String(appNameRecord.value(), StandardCharsets.UTF_8) : "",
                    new String(retry.value(), StandardCharsets.UTF_8)
            );
        }
        return out;
    }

    /** The name of the business logic flow to print when logging */
    protected abstract String getFlowName();


    /** In case of errors reading a message */
    protected abstract void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception);

    protected E deserialize(String payload) throws JsonProcessingException {
        return objectReader.readValue(payload);
    }

}
