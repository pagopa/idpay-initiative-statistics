package it.gov.pagopa.initiative.statistics.service.commands.ops;

import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
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
class CreateInitiativeStatisticsServiceImplTest {

    @Mock
    private InitiativeStatRepository initiativeStatRepository;

    @InjectMocks
    private CreateInitiativeStatisticsServiceImpl service;

    @Test
    void shouldDoNothingWhenEntityIdIsNull() {
        service.execute(null);

        verifyNoInteractions(initiativeStatRepository);
    }

    @Test
    void shouldDoNothingWhenEntityIdIsEmpty() {
        service.execute("");

        verifyNoInteractions(initiativeStatRepository);
    }

    @Test
    void shouldDoNothingWhenStatisticsAlreadyExist() {

        String entityId = "INIT_ORG";

        InitiativeStatistics existing = InitiativeStatistics.builder()
                .initiativeId("INIT")
                .organizationId("ORG")
                .build();

        when(initiativeStatRepository.findById("INIT"))
                .thenReturn(Optional.of(existing));

        service.execute(entityId);

        verify(initiativeStatRepository).findById("INIT");
        verify(initiativeStatRepository, never()).save(any());
    }

    @Test
    void shouldCreateStatisticsWhenNotExists() {

        String entityId = "INIT_ORG";

        when(initiativeStatRepository.findById("INIT"))
                .thenReturn(Optional.empty());

        service.execute(entityId);

        ArgumentCaptor<InitiativeStatistics> captor =
                ArgumentCaptor.forClass(InitiativeStatistics.class);

        verify(initiativeStatRepository).save(captor.capture());

        InitiativeStatistics saved = captor.getValue();

        assertEquals("INIT", saved.getInitiativeId());
        assertEquals("ORG", saved.getOrganizationId());
        assertNotNull(saved.getCreatedAt());
    }
}