package com.bofa.mobile.util;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class MoneyTest {

    @Test
    public void formatsUsdWithGrouping() {
        assertEquals("$4,812.33", Money.format("4812.33"));
    }

    @Test
    public void formatsNegativeAmounts() {
        assertEquals("-$42.18", Money.format("-42.18"));
    }

    @Test
    public void wireFormatHasTwoPlaces() {
        assertEquals("250.00", Money.toWire(new BigDecimal("250")));
    }
}
