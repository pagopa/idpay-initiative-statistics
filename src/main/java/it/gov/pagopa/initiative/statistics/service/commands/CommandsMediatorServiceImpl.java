package it.gov.pagopa.initiative.statistics.service.commands;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.initiative.statistics.dto.events.CommandOperationDTO;
import it.gov.pagopa.initiative.statistics.service.BaseGenericConsumerService;
import it.gov.pagopa.initiative.statistics.service.StatisticsErrorNotifierService;
import it.gov.pagopa.initiative.statistics.service.commands.ops.CreateInitiativeStatisticsService;
import it.gov.pagopa.initiative.statistics.service.commands.ops.CreateMerchantCountersService;
import it.gov.pagopa.initiative.statistics.service.commands.ops.DeleteInitiativeService;
import it.gov.pagopa.initiative.statistics.utils.CommandsConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommandsMediatorServiceImpl extends BaseGenericConsumerService<CommandOperationDTO> implements CommandsMediatorService {
    private final DeleteInitiativeService deleteInitiativeService;
    private final StatisticsErrorNotifierService statisticsErrorNotifierService;
    private final CreateInitiativeStatisticsService createInitiativeStatisticsService;
    private final CreateMerchantCountersService createMerchantCountersService;

    protected CommandsMediatorServiceImpl(@Value("${spring.application.name}") String applicationName,
                                          @Value("${app.kafka.consumer.commands.group-id}") String consumerGroup,
                                          ObjectMapper objectMapper,
                                          DeleteInitiativeService deleteInitiativeService,
                                          StatisticsErrorNotifierService statisticsErrorNotifierService,
                                          CreateInitiativeStatisticsService createInitiativeStatisticsService,
                                          CreateMerchantCountersService createMerchantCountersService) {
        super(applicationName, consumerGroup, objectMapper);
        this.deleteInitiativeService = deleteInitiativeService;
        this.statisticsErrorNotifierService = statisticsErrorNotifierService;
        this.createInitiativeStatisticsService = createInitiativeStatisticsService;
        this.createMerchantCountersService = createMerchantCountersService;
    }

    @Override
    protected Class<CommandOperationDTO> getRecordClass() {
        return CommandOperationDTO.class;
    }

    @Override
    protected String getFlowName() {
        return "INITIATIVE_STATISTICS_COMMANDS";
    }

    @Override
    protected void onDeserializeError(ConsumerRecord<String, String> message, String description, Throwable exception) {
        statisticsErrorNotifierService.notifyCommandsOperation(message, description,false, exception);
    }

    @Override
    protected void onRecordError2notify(ConsumerRecord<String, String> message, String description, Throwable exception) {
        statisticsErrorNotifierService.notifyCommandsOperation(message, description,true, exception);
    }

    @Override
    protected void evaluate(CommandOperationDTO payload) {
        if (CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE.equals(payload.getOperationType())){
            deleteInitiativeService.execute(payload);
        } else if(CommandsConstants.COMMANDS_OPERATION_TYPE_CREATE_INITIATIVE_STATISTICS.equals(payload.getOperationType())) {
            createInitiativeStatisticsService.execute(payload.getEntityId());
        } else if(CommandsConstants.COMMANDS_OPERATION_TYPE_CREATE_MERCHANT_STATISTICS.equals(payload.getOperationType())) {
            createMerchantCountersService.execute(payload.getEntityId());
        } else {
            log.debug("[INITIATIVE_STATISTICS_COMMANDS] Not handled operation type {}", payload.getOperationType());
        }
    }
}
