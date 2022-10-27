package it.gov.pagopa.initiative.statistics.controller;

import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.InitiativeStatServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@WebMvcTest(InitiativeApiControllerImpl.class)
@Import(InitiativeStatServiceImpl.class)
class InitiativeApiControllerTest {

    @MockBean
    private InitiativeStatRepository repositoryMock;

    @Autowired
    private MockMvc mvc;

    @Test
    void testInitiativeStatisticsSuccessful() throws Exception {
        InitiativeStatistics mockedEntity = InitiativeStatistics.builder()
                .initiativeId("INITIATIVEID")
                .organizationId("ORGANIZATIONID")
                .onboardedCitizenCount(7L)
                .accruedRewardsCents(537L)
                .lastUpdatedDateTime(LocalDateTime.of(LocalDate.of(2022, 10, 1), LocalTime.MIDNIGHT))
                .build();

        Mockito.when(repositoryMock.findById("INITIATIVEID")).thenReturn(Optional.of(mockedEntity));

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/idpay/organization/ORGANIZATIONID/initiative/INITIATIVEID/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn();

        Assertions.assertEquals("{\"lastUpdatedDateTime\":\"2022-10-01T00:00:00\",\"onboardedCitizenCount\":7,\"accruedRewards\":\"5,37\"}", result.getResponse().getContentAsString());


        // When not onboarding count
        mockedEntity.setOnboardedCitizenCount(null);
        MvcResult result2 = mvc.perform(MockMvcRequestBuilders
                        .get("/idpay/organization/ORGANIZATIONID/initiative/INITIATIVEID/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn();

        Assertions.assertEquals("{\"lastUpdatedDateTime\":\"2022-10-01T00:00:00\",\"onboardedCitizenCount\":0,\"accruedRewards\":\"5,37\"}", result2.getResponse().getContentAsString());

        // When not accrued rewards
        mockedEntity.setOnboardedCitizenCount(7L);
        mockedEntity.setAccruedRewardsCents(null);
        MvcResult result3 = mvc.perform(MockMvcRequestBuilders
                        .get("/idpay/organization/ORGANIZATIONID/initiative/INITIATIVEID/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn();

        Assertions.assertEquals("{\"lastUpdatedDateTime\":\"2022-10-01T00:00:00\",\"onboardedCitizenCount\":7,\"accruedRewards\":\"0,00\"}", result3.getResponse().getContentAsString());
    }

    @Test
    void testInitiativeStatistics404() throws Exception {
        Mockito.when(repositoryMock.findById(Mockito.any())).thenReturn(Optional.empty());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/idpay/organization/ORGANIZATIONID/initiative/INITIATIVEID/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andReturn();

        Assertions.assertEquals(404, result.getResponse().getStatus());
    }
}
