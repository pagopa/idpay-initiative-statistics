package it.gov.pagopa.initiative.statistics.events.consumers;

import com.mongodb.MongoException;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import it.gov.pagopa.initiative.statistics.dto.events.CommandOperationDTO;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.utils.CommandsConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.initiative.statistics.service.commands.ops.DeleteInitiativeServiceImpl=WARN",
        "logging.level.it.gov.pagopa.initiative.statistics.service.commands.CommandsMediatorServiceImpl=WARN",
})
class CommandsMessagesListenerTest extends BaseIntegrationTest {
    private final String INITIATIVEID = "INITIATIVEID_%d";
    private final Set<String> INITIATIVES_DELETED = new HashSet<>();
    @SpyBean
    private InitiativeStatRepository initiativeStatRepository;
    @Autowired
    private MerchantInitiativeCountersRepository merchantInitiativeCountersRepository;

    @Test
    void test() {
        int validMessages = 100;
        int notValidMessages = errorUseCasesNotify.size();
        long maxWaitingMs = 30000;

        List<String> commandsPayloads = new ArrayList<>(notValidMessages+validMessages);
        commandsPayloads.addAll(IntStream.range(0,notValidMessages).mapToObj(i -> errorUseCasesNotify.get(i).getFirst().get()).toList());
        commandsPayloads.addAll(buildValidPayloads(notValidMessages, notValidMessages+validMessages));

        long timeStart=System.currentTimeMillis();
        commandsPayloads.forEach(cp -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicCommands, null, null, cp));
        long timePublishingEnd = System.currentTimeMillis();

        waitForLastStorageChange(validMessages/2);
        long timeEnd=System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) messages (from start): %d millis
                        Time spent to assert db stored count (from previous check): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                commandsPayloads.size(),
                validMessages,
                notValidMessages,
                timePublishingEnd - timeStart,
                timeEnd - timePublishingEnd,
                timeEnd - timeStart
        );

        checkRepositories();
        checkErrorsPublished(notValidMessages, maxWaitingMs, errorUseCasesNotify);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) messages (from start): %d millis
                        Time spent to assert db stored count (from previous check): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                commandsPayloads.size(),
                validMessages,
                notValidMessages,
                timePublishingEnd - timeStart,
                timeEnd - timePublishingEnd,
                timeEnd - timeStart
        );
    }

    private long waitForLastStorageChange(int n) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        TestUtils.waitFor(()->(countSaved[0]=merchantInitiativeCountersRepository.count()) == n, ()->"Expected %d saved merchant counters in db, read %d, DB elements %s".formatted(n, countSaved[0], merchantInitiativeCountersRepository.findAll().toString()), 60, 2000);
        return countSaved[0];
    }

    private List<String> buildValidPayloads(int startValue, int messagesNumber) {
        return IntStream.range(startValue, messagesNumber)
                .mapToObj(i -> {
                    initializeDB(i);
                    CommandOperationDTO command = CommandOperationDTO.builder()
                            .entityId(INITIATIVEID.formatted(i))
                            .operationTime(LocalDateTime.now())
                            .build();

                    if(i%2 == 0){
                        INITIATIVES_DELETED.add(command.getEntityId());
                        command.setOperationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE);
                    } else {
                        command.setOperationType("ANOTHER_TYPE");
                    }
                    return command;
                })
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private void initializeDB(int bias) {
        String initiativeId = INITIATIVEID.formatted(bias);
        InitiativeStatistics initiativeStatistics = InitiativeStatistics.builder()
                .initiativeId(initiativeId)
                .organizationId("ORGANIZATIONID_%d".formatted(bias))
                .lastUpdatedDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .onboardedCitizenCount(10L)
                .build();
        initiativeStatRepository.save(initiativeStatistics);

        MerchantInitiativeCounters merchantInitiativeCounters = MerchantInitiativeCounters
                .builder("MERCHANTID_%d".formatted(bias), initiativeId)
                .build();
        merchantInitiativeCountersRepository.save(merchantInitiativeCounters);

    }

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"entityId\":\"ENTITYID_ERROR([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCasesNotify = new ArrayList<>();

    {
        String useCaseJsonNotExpected = "{\"entityId\":\"ENTITYID_ERROR0\",unexpectedStructure:0}";
        errorUseCasesNotify.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage,
                        "[INITIATIVE_STATISTICS_COMMANDS] Unexpected json: %s".formatted(useCaseJsonNotExpected),
                        useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"entityId\":\"ENTITYID_ERROR1\",invalidJson";
        errorUseCasesNotify.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage,
                        "[INITIATIVE_STATISTICS_COMMANDS] Unexpected json: %s".formatted(jsonNotValid),
                        jsonNotValid)
        ));

        final String errorInitiativeId = "ENTITYID_ERROR2";
        CommandOperationDTO commandOperationError = CommandOperationDTO.builder()
                .entityId(errorInitiativeId)
                .operationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE)
                .operationTime(LocalDateTime.now())
                .build();
        String commandOperationErrorString = TestUtils.jsonSerializer(commandOperationError);
        errorUseCasesNotify.add(Pair.of(
                () -> {
                    Mockito.doThrow(new MongoException("Command error dummy"))
                            .when(initiativeStatRepository).deleteById(errorInitiativeId);
                    return commandOperationErrorString;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage,
                        "[INITIATIVE_STATISTICS_COMMANDS] Something gone wrong during the evaluation of the payload: %s".formatted(commandOperationErrorString),
                        commandOperationErrorString)
        ));
    }

    private void checkRepositories() {
        Assertions.assertTrue(initiativeStatRepository.findAll().stream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
        Assertions.assertTrue(merchantInitiativeCountersRepository.findAll().stream().noneMatch(ri -> INITIATIVES_DELETED.contains(ri.getInitiativeId())));
    }
    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicCommands, groupIdCommands, errorMessage, errorDescription, expectedPayload, null);
    }
}