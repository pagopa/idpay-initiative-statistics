package it.gov.pagopa.initiative.statistics.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.initiative.statistics.BaseIntegrationTest;
import it.gov.pagopa.initiative.statistics.controller.MerchantPortalController;
import it.gov.pagopa.initiative.statistics.model.MerchantInitiativeCounters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

class ErrorManagerTest extends BaseIntegrationTest {

    @MockBean
    private MerchantPortalController controller;

    @Autowired
    private MockMvc mvc;

    @Test
    void handleExceptionClientExceptionNoBody() throws Exception {

        Mockito.when(controller.getMerchantInitiativeStatistics("ClientExceptionNoBody", "INITIATIVE_ID"))
                .thenThrow(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "NOTFOUND"));

        mvc.perform(MockMvcRequestBuilders.get("/idpay/merchant/portal/initiatives/{initiativeId}/statistics", "INITIATIVE_ID")
                        .header("x-merchant-id", "ClientExceptionNoBody"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void handleExceptionClientExceptionWithBody() throws Exception {

        Mockito.when(controller.getMerchantInitiativeStatistics("ClientExceptionWithBody", "INITIATIVE_ID"))
                .thenThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody"));
        ErrorDTO errorClientExceptionWithBody= new ErrorDTO("Error","Error ClientExceptionWithBody");

        mvc.perform(MockMvcRequestBuilders.get("http://localhost:8080/idpay/merchant/portal/initiatives/{initiativeId}/statistics", "INITIATIVE_ID")
                        .header("x-merchant-id", "ClientExceptionWithBody"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(r ->
                        Assertions.assertEquals(
                                "{\"code\":\"Error\",\"message\":\"Error ClientExceptionWithBody\"}",
                                r.getResponse().getContentAsString()
                        ));


        Mockito.when(controller.getMerchantInitiativeStatistics("ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable", "INITIATIVE_ID"))
                .thenThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody", new Throwable()));
        ErrorDTO errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable = new ErrorDTO("Error","Error ClientExceptionWithBody");

        mvc.perform(MockMvcRequestBuilders.get("/idpay/merchant/portal/initiatives/{initiativeId}/statistics", "INITIATIVE_ID")
                        .header("x-merchant-id", "ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(r ->
                        Assertions.assertEquals(
                                "{\"code\":\"Error\",\"message\":\"Error ClientExceptionWithBody\"}",
                                r.getResponse().getContentAsString()
                        ));
    }

    @Test
    void handleExceptionClientExceptionTest() throws Exception {

        Mockito.when(controller.getMerchantInitiativeStatistics("ClientException", "INITIATIVE_ID"))
                .thenThrow(ClientException.class);
        mvc.perform(MockMvcRequestBuilders.get("/idpay/merchant/portal/initiatives/{initiativeId}/statistics", "INITIATIVE_ID")
                        .header("x-merchant-id", "ClientException"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError());


        Mockito.when(controller.getMerchantInitiativeStatistics("ClientExceptionStatusAndMessage", "INITIATIVE_ID"))
                .thenThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus and message"));
        mvc.perform(MockMvcRequestBuilders.get("/idpay/merchant/portal/initiatives/{initiativeId}/statistics", "INITIATIVE_ID")
                        .header("x-merchant-id", "ClientExceptionStatusAndMessage"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(r -> Assertions.assertEquals(
                        "{\"code\":\"Error\",\"message\":\"Something gone wrong\"}",
                        r.getResponse().getContentAsString()
                ));

        Mockito.when(controller.getMerchantInitiativeStatistics("ClientExceptionStatusAndMessageAndThrowable", "INITIATIVE_ID"))
                .thenThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus, message and throwable", new Throwable()));
        mvc.perform(MockMvcRequestBuilders.get("/idpay/merchant/portal/initiatives/{initiativeId}/statistics", "INITIATIVE_ID")
                        .header("x-merchant-id", "ClientExceptionStatusAndMessageAndThrowable"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(r -> Assertions.assertEquals(
                        "{\"code\":\"Error\",\"message\":\"Something gone wrong\"}",
                        r.getResponse().getContentAsString()
                ));
    }

    @Test
    void handleExceptionRuntimeException() throws Exception {

        Mockito.when(controller.getMerchantInitiativeStatistics("RuntimeException", "INITIATIVE_ID"))
                .thenThrow(RuntimeException.class);
        mvc.perform(MockMvcRequestBuilders.get("/idpay/merchant/portal/initiatives/{initiativeId}/statistics", "INITIATIVE_ID")
                        .header("x-merchant-id", "RuntimeException"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError());
    }

    // unused
    @Override
    protected String buildCounterId(String initiativeId) {
        return MerchantInitiativeCounters.buildId("MERCHANTID", initiativeId);
    }
}