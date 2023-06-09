package it.gov.pagopa.initiative.statistics.utils;

import java.util.List;

public final class Constants {
    private Constants(){}

    //region trx constants
    public static final String TRX_STATUS_AUTHORIZED = "AUTHORIZED";
    public static final String TRX_STATUS_CANCELLED = "CANCELLED";

    public static final List<String> EXCLUDED_TRX_STATUSES = List.of(TRX_STATUS_AUTHORIZED, TRX_STATUS_CANCELLED);
    //endregion
}
