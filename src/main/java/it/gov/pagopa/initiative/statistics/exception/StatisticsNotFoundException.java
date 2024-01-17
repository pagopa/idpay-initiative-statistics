package it.gov.pagopa.initiative.statistics.exception;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.initiative.statistics.constants.InitiativeConstants;

public class StatisticsNotFoundException extends ServiceException {

    public StatisticsNotFoundException(String message) {
        this(InitiativeConstants.ExceptionCode.STATISTICS_NOT_FOUND, message);
    }

    public StatisticsNotFoundException(String code, String message) {
        this(code, message,null, false, null);
    }

    public StatisticsNotFoundException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
