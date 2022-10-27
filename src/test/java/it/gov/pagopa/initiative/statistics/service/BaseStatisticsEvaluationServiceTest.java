package it.gov.pagopa.initiative.statistics.service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class BaseStatisticsEvaluationServiceTest {

    protected static final String TOPIC_NAME = "TOPIC";
    public static final int EXPECTED_PARTITION3_OFFSET = 11;

    protected int expectedErrorNotification = 2;

    public void invokeService(String applicationName, Consumer<?,?> consumerMock, int partition0Offset, int partition1Offset, ErrorNotifierService errorNotifierServiceMock){
        Mockito.doAnswer(i -> {
                    Map<TopicPartition, OffsetAndMetadata> commit = i.getArgument(0);
                    // it will mock an exception when committing partition 1
                    Exception exceptionWhenCommit = commit.keySet().stream().anyMatch(t->t.partition()==3) ? new RuntimeException("DUMMYEXCEPTION") : null;
                    ((OffsetCommitCallback) i.getArgument(1)).onComplete(commit, exceptionWhenCommit);
                    return null;
                })
                .when(consumerMock)
                .commitAsync(Mockito.anyMap(), Mockito.notNull());

        List<String> useCases = getUseCases();

        // sending already committed offsets
        List<ConsumerRecord<String, String>> records = new ArrayList<>(
                useCases.stream().map(p -> new ConsumerRecord<>(TOPIC_NAME, 0, -1, (String)null, p)).toList()
        );
        // sending retried messages by other applications
        records.addAll(
                useCases.stream().map(p -> {
                    ConsumerRecord<String, String> message = new ConsumerRecord<>(TOPIC_NAME, 0, -1, null, p);
                    message.headers().add("retry", "1".getBytes(StandardCharsets.UTF_8));
                    return message;
                }).toList()
        );
        // sending invalid messages (expectedErrorNotification + 1) -> partition 2 will not committed, so not error will be notified through topic, it will just be logged if not other valid messages follow it
        records.addAll(List.of(
                new ConsumerRecord<>(TOPIC_NAME, 2, 1, null, "INVALIDJSON AS LAST MESSAGE OF A PARTITION"),
                new ConsumerRecord<>(TOPIC_NAME, 0, ++partition0Offset, null, "INVALIDJSON"),
                new ConsumerRecord<>(TOPIC_NAME, 0, ++partition0Offset, "KEY", "{invalidStructure:\"\"}")
        ));
        // sending on partition whose commit will fail
        records.addAll(
                useCases.stream().map(p -> new ConsumerRecord<>(TOPIC_NAME, 3, EXPECTED_PARTITION3_OFFSET, (String)null, p)).toList()
        );
        // sending valid use cases
        for (int i = 0; i < useCases.size(); i++) {
            ConsumerRecord<String, String> message = new ConsumerRecord<>(TOPIC_NAME, i % 2, i % 2 == 0 ? ++partition0Offset : ++partition1Offset, null, useCases.get(i));
            if(i%3==0){
                message.headers().add("retry", "1".getBytes(StandardCharsets.UTF_8));
                message.headers().add(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, applicationName.getBytes(StandardCharsets.UTF_8));
            }
            records.add(message);
        }

        StatisticsEvaluationService service = getStatisticsEvaluationServiceImpl();
        service.evaluate(records, consumerMock);

        Assertions.assertEquals(expectedErrorNotification,
                Mockito.mockingDetails(errorNotifierServiceMock).getInvocations().size(),
                "Unexpected number of errorNotifierService invocations: %s".formatted(Mockito.mockingDetails(errorNotifierServiceMock).getInvocations())
        );

        Mockito.mockingDetails(errorNotifierServiceMock).getInvocations()
                .forEach(i-> System.out.println("Called errorNotifier: " + Arrays.toString(i.getArguments())));

        verifyResults(partition0Offset, partition1Offset);
    }

    protected abstract StatisticsEvaluationService getStatisticsEvaluationServiceImpl();

    protected abstract List<String> getUseCases();

    protected abstract void verifyResults(int partition0LastCommittedOffset, int partition1LastCommittedOffset);
}
