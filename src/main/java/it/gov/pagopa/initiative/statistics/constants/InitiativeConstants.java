package it.gov.pagopa.initiative.statistics.constants;


public class InitiativeConstants {

    private InitiativeConstants(){}

    public static final class ExceptionCode {
        public static final String STATISTICS_NOT_FOUND = "STATISTICS_NOT_FOUND";
        public static final String GENERIC_ERROR = "STATISTICS_GENERIC_ERROR";
        public static final String TOO_MANY_REQUESTS = "STATISTICS_TOO_MANY_REQUESTS";
        public static final String INVALID_REQUEST = "STATISTICS_INVALID_REQUEST";

        private ExceptionCode() {}
    }

}
