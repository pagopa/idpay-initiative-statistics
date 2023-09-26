package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.dto.events.CommandOperationDTO;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.utils.AuditUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteInitiativeServiceImplTest {
    private DeleteInitiativeService deleteInitiativeService;
    @Mock
    private InitiativeStatRepository initiativeStatRepository;
    @Mock
    private MerchantInitiativeCountersRepository merchantInitiativeCountersRepository;
    @Mock
    private AuditUtilities auditUtilities;
    private static final String PAGINATION_KEY = "pagination";
    private static final String DELAY_KEY = "delay";
    private static final String PAGINATION_VALUE = "100";
    private static final String DELAY_VALUE = "1500";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String MERCHANT_INITIATIVE_COUNTERS_ID = "INITIATIVE_COUNTER_IS";
    private static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";

    @BeforeEach
    void setUp() {
        deleteInitiativeService = new DeleteInitiativeServiceImpl(initiativeStatRepository, merchantInitiativeCountersRepository, auditUtilities);
    }

    @Test
    void processCommand_deleteTransactions() {
        // Given
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(PAGINATION_KEY, PAGINATION_VALUE);
        additionalParams.put(DELAY_KEY, DELAY_VALUE);
        final CommandOperationDTO queueCommandOperationDTO = CommandOperationDTO.builder()
                .entityId(INITIATIVE_ID)
                .operationType(OPERATION_TYPE_DELETE_INITIATIVE)
                .operationTime(LocalDateTime.now().minusMinutes(5))
                .additionalParams(additionalParams)
                .build();
        MerchantInitiativeCounters merchantInitiativeCounters = new MerchantInitiativeCounters();
        merchantInitiativeCounters.setId(MERCHANT_INITIATIVE_COUNTERS_ID);
        merchantInitiativeCounters.setInitiativeId(INITIATIVE_ID);
        final List<MerchantInitiativeCounters> deletedPage = List.of(merchantInitiativeCounters);

        final List<MerchantInitiativeCounters> merchantInitiativeCountersPage = createMerchantInitiativeCountersPage(Integer.parseInt(PAGINATION_VALUE));
        when(merchantInitiativeCountersRepository.deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(queueCommandOperationDTO.getAdditionalParams().get(PAGINATION_KEY))))
                .thenReturn(merchantInitiativeCountersPage)
                .thenReturn(deletedPage);
        Thread.currentThread().interrupt();

        // When
        deleteInitiativeService.execute(queueCommandOperationDTO);

        // Then
        Mockito.verify(merchantInitiativeCountersRepository, Mockito.times(2)).deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(queueCommandOperationDTO.getAdditionalParams().get(PAGINATION_KEY)));
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
