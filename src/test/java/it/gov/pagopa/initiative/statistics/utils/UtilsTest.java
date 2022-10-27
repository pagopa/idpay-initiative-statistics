package it.gov.pagopa.initiative.statistics.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class UtilsTest {
    @Test
    void testEuro2Cents(){
        Assertions.assertNull(Utils.euro2Cents(null));
        Assertions.assertEquals(100L, Utils.euro2Cents(BigDecimal.ONE));
        Assertions.assertEquals(325L, Utils.euro2Cents(BigDecimal.valueOf(3.25)));
    }
}
