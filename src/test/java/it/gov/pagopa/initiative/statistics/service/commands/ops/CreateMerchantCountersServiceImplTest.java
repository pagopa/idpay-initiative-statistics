package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateMerchantCountersServiceImplTest {

    @Mock
    private MerchantInitiativeCountersRepository repository;

    @InjectMocks
    private CreateMerchantCountersServiceImpl service;

    @Test
    void shouldDoNothingWhenEntityIdIsNull() {
        service.execute(null);

        verifyNoInteractions(repository);
    }

    @Test
    void shouldDoNothingWhenEntityIdIsEmpty() {
        service.execute("");

        verifyNoInteractions(repository);
    }

    @Test
    void shouldDoNothingWhenCountersAlreadyExist() {

        String entityId = "INIT_MERCH";

        String initiativeId = "INIT";
        String merchantId = "MERCH";

        String counterId = MerchantInitiativeCounters.buildId(merchantId, initiativeId);

        MerchantInitiativeCounters existing =
                new MerchantInitiativeCounters(merchantId, initiativeId);

        when(repository.findById(counterId))
                .thenReturn(Optional.of(existing));

        service.execute(entityId);

        verify(repository).findById(counterId);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldCreateCountersWhenNotExists() {

        String entityId = "INIT_MERCH";

        String initiativeId = "INIT";
        String merchantId = "MERCH";

        String expectedCounterId =
                MerchantInitiativeCounters.buildId(merchantId, initiativeId);

        when(repository.findById(expectedCounterId))
                .thenReturn(Optional.empty());

        service.execute(entityId);

        ArgumentCaptor<MerchantInitiativeCounters> captor =
                ArgumentCaptor.forClass(MerchantInitiativeCounters.class);

        verify(repository).save(captor.capture());

        MerchantInitiativeCounters saved = captor.getValue();

        assertEquals(merchantId, saved.getMerchantId());
        assertEquals(initiativeId, saved.getInitiativeId());
    }
}