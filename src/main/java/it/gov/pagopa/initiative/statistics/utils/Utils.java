package it.gov.pagopa.initiative.statistics.utils;

import java.math.BigDecimal;

public final class Utils {
    private Utils(){}

    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public static Long euro2Cents(BigDecimal euro){
        return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
    }
}
