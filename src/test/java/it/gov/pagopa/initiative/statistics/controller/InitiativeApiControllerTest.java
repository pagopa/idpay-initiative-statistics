package it.gov.pagopa.initiative.statistics.controller;

import it.gov.pagopa.initiative.statistics.config.ServiceExceptionConfig;
import it.gov.pagopa.initiative.statistics.model.InitiativeStatistics;
import it.gov.pagopa.initiative.statistics.repository.InitiativeStatRepository;
import it.gov.pagopa.initiative.statistics.service.InitiativeStatServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@WebMvcTest(value = {
        InitiativeApiController.class, ServiceExceptionConfig.class}, excludeAutoConfiguration =  { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(InitiativeStatServiceImpl.class)
class InitiativeApiControllerTest {

    @MockitoBean
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
                .rewardedTrxs(13L)
                .lastUpdatedDateTime(LocalDateTime.of(LocalDate.of(2022, 10, 1), LocalTime.MIDNIGHT))
                .build();

        Mockito.when(repositoryMock.findById("INITIATIVEID")).thenReturn(Optional.of(mockedEntity));

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/idpay/organization/ORGANIZATIONID/initiative/INITIATIVEID/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn();

        Assertions.assertEquals("{\"lastUpdatedDateTime\":\"2022-10-01T00:00:00\",\"onboardedCitizenCount\":7,\"rewardedTrxs\":13,\"accruedRewards\":5.37}", result.getResponse().getContentAsString());
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
