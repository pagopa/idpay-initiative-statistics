package it.gov.pagopa.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
}
