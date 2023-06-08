package it.gov.pagopa.initiative.statistics.controller;

import it.gov.pagopa.initiative.statistics.dto.mapper.MerchantInitiativeCounters2MerchantInitiativeStatisticsDTOMapper;
import it.gov.pagopa.initiative.statistics.model.CommittedOffset;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import it.gov.pagopa.initiative.statistics.repository.merchant.counters.MerchantInitiativeCountersRepository;
import it.gov.pagopa.initiative.statistics.service.merchant.counters.MerchantCountersServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.Optional;

@WebMvcTest(MerchantPortalControllerImpl.class)
@Import({MerchantCountersServiceImpl.class, MerchantInitiativeCounters2MerchantInitiativeStatisticsDTOMapper.class})
class MerchantPortalControllerImplTest {

    @MockBean
    private MerchantInitiativeCountersRepository repositoryMock;

    @Autowired
    private MockMvc mvc;

    @Test
    void testSuccessful() throws Exception {
        List<CommittedOffset> mockedOffsets = List.of(new CommittedOffset(1, 1));

        MerchantInitiativeCounters mockedEntity = MerchantInitiativeCounters.builder("MERCHANTID", "INITIATIVEID")
                .totalProvidedCents(10000L)
                .totalRefundedCents(100L)
                .trxNumber(5)
                .refundedNumber(1)
                .trxCommittedOffsets(mockedOffsets)
                .rewardNotificationCommittedOffsets(mockedOffsets)
                .build();

        Mockito.when(repositoryMock.findById("MERCHANTID_INITIATIVEID")).thenReturn(Optional.of(mockedEntity));

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/idpay/merchant/portal/initiatives/INITIATIVEID/statistics")
                        .header("x-merchant-id", "MERCHANTID")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andReturn();

        Assertions.assertEquals("{\"amount\":100.00,\"accrued\":99.00,\"refunded\":1.00}", result.getResponse().getContentAsString());
    }

    @Test
    void test404() throws Exception {
        Mockito.when(repositoryMock.findById(Mockito.any())).thenReturn(Optional.empty());

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/idpay/merchant/portal/initiatives/INITIATIVEID/statistics")
                        .header("x-merchant-id", "MERCHANTID")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andReturn();

        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

}