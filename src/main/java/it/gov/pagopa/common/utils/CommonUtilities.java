package it.gov.pagopa.common.utils;

import java.math.BigDecimal;

public final class CommonUtilities {
    private CommonUtilities(){}

    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    /** To convert euro into cents */
    public static Long euroToCents(BigDecimal euro){
        return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
    }
}
