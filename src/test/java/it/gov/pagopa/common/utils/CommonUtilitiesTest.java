package it.gov.pagopa.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

class CommonUtilitiesTest {
    @Test
    void testEuroToCents(){
        Assertions.assertNull(CommonUtilities.euroToCents(null));
        Assertions.assertEquals(100L, CommonUtilities.euroToCents(BigDecimal.ONE));
        Assertions.assertEquals(325L, CommonUtilities.euroToCents(BigDecimal.valueOf(3.25)));

        Assertions.assertEquals(
                5_00L,
                CommonUtilities.euroToCents(TestUtils.bigDecimalValue(5))
        );
    }

    @Test
    void testCentsToEuro(){
        Assertions.assertEquals(BigDecimal.ONE.setScale(2, RoundingMode.HALF_DOWN), CommonUtilities.centsToEuro(100L));
        Assertions.assertEquals(BigDecimal.valueOf(3.25).setScale(2, RoundingMode.HALF_DOWN), CommonUtilities.centsToEuro(325L));
    }
}
