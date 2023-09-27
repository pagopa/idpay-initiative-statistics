package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.dto.events.CommandOperationDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.utils.AuditUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(classes = DeleteInitiativeServiceImpl.class)
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {
                "app.delete.paginationSize=100",
                "app.delete.delayTime=1000"
        })
class DeleteInitiativeServiceImplTest {
    @Autowired
    private DeleteInitiativeService deleteInitiativeService;
    @MockBean
    private InitiativeStatRepository initiativeStatRepository;
    @MockBean
    private MerchantInitiativeCountersRepository merchantInitiativeCountersRepository;
    @MockBean
    private AuditUtilities auditUtilities;
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String MERCHANT_INITIATIVE_COUNTERS_ID = "INITIATIVE_COUNTER_IS";
    private static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";

    @Value("${app.delete.paginationSize}")
    String pagination;

    @Test
    void processCommand_deleteTransactions() {
        // Given
        final CommandOperationDTO queueCommandOperationDTO = CommandOperationDTO.builder()
                .entityId(INITIATIVE_ID)
                .operationType(OPERATION_TYPE_DELETE_INITIATIVE)
                .operationTime(LocalDateTime.now().minusMinutes(5))
                .build();
        MerchantInitiativeCounters merchantInitiativeCounters = new MerchantInitiativeCounters();
        merchantInitiativeCounters.setId(MERCHANT_INITIATIVE_COUNTERS_ID);
        merchantInitiativeCounters.setInitiativeId(INITIATIVE_ID);
        final List<MerchantInitiativeCounters> deletedPage = List.of(merchantInitiativeCounters);

        final List<MerchantInitiativeCounters> merchantInitiativeCountersPage = createMerchantInitiativeCountersPage(Integer.parseInt(pagination));
        when(merchantInitiativeCountersRepository.deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(pagination)))
                .thenReturn(merchantInitiativeCountersPage)
                .thenReturn(deletedPage);
        Thread.currentThread().interrupt();

        // When
        deleteInitiativeService.execute(queueCommandOperationDTO);

        // Then
        Mockito.verify(merchantInitiativeCountersRepository, Mockito.times(2)).deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(pagination));
    }

    private List<MerchantInitiativeCounters> createMerchantInitiativeCountersPage(int pageSize){
        List<MerchantInitiativeCounters> merchantInitiativeCountersPage = new ArrayList<>();

        for(int i=0;i<pageSize; i++){
            MerchantInitiativeCounters merchantInitiativeCounters = new MerchantInitiativeCounters();
            merchantInitiativeCounters.setId(MERCHANT_INITIATIVE_COUNTERS_ID);
            merchantInitiativeCounters.setInitiativeId(INITIATIVE_ID);
            merchantInitiativeCountersPage.add(merchantInitiativeCounters);
        }

        return merchantInitiativeCountersPage;
    }

}
